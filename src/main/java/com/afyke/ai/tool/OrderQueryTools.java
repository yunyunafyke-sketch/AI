package com.afyke.ai.tool;

import com.afyke.ai.order.service.OrderQueryResult;
import com.afyke.ai.order.service.OrderQueryService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderQueryTools {

    private final Validator validator;
    private final OrderQueryService orderQueryService;

    public OrderQueryTools(
            Validator validator,
            OrderQueryService orderQueryService) {
        // Validator 负责校验模型生成的参数。
        this.validator = validator;
        // Service 负责真正的订单查询业务。
        this.orderQueryService = orderQueryService;
    }

    @Tool(
            name = "queryOrderStatus",
            description = "根据订单编号查询当前订单状态；用户询问订单是否付款、发货、完成或取消时使用；只读，不修改订单"
    )
    public ToolResult<OrderQueryResult> queryOrderStatus(
            @ToolParam(
                    description = "订单编号，格式为大写字母 A 加 4～10 位数字，例如 A1001",
                    required = true
            )
            String orderId,
            ToolContext toolContext) {

        // ToolContext 中的数据来自 Java 应用，不由模型生成。
        Object userIdValue = toolContext.getContext().get("userId");
        String userId = userIdValue == null ? "" : userIdValue.toString();

        if (userId.isBlank()) {
            return ToolResult.failure(
                    "CONTEXT_MISSING",
                    "缺少调用用户上下文",
                    List.of("Java 应用必须通过 ToolContext 提供 userId")
            );
        }

        // Tool 入口只做边界处理，再把业务查询交给内部方法和 Service。
        return queryOrderStatusInternal(orderId);
    }

    /**
     * 内部方法不依赖 ToolContext，便于对参数和业务结果做确定性单元测试。
     */
    ToolResult<OrderQueryResult> queryOrderStatusInternal(String orderId) {
        QueryOrderCommand command = new QueryOrderCommand(orderId);

        // validate：执行 @NotBlank 和 @Pattern 约束。
        var violations = validator.validate(command);

        if (!violations.isEmpty()) {
            List<String> errors = violations.stream()
                    // getMessage：只提取受控错误信息，不把异常堆栈交给模型。
                    .map(ConstraintViolation::getMessage)
                    // sorted：让错误顺序稳定，方便测试比较。
                    .sorted()
                    .toList();

            // 参数未通过时立即结束，不调用订单 Service。
            return ToolResult.failure(
                    "INVALID_ARGUMENT",
                    "工具参数校验失败",
                    errors
            );
        }

        // queryOrderStatus 返回 Optional<OrderQueryResult>：
        // 查到订单时 Optional 中有结果；订单不存在时得到 Optional.empty()。
        return orderQueryService.queryOrderStatus(command.orderId())
                // map：Optional 中有订单结果时才执行，把它包装成 Tool 的成功结果。
                .map(order -> ToolResult.success("订单状态查询成功", order))
                // orElseGet：Optional 为空时才执行，返回稳定的“订单不存在”结果。
                // 这里使用 orElseGet 而不是直接取值，可以安全处理订单不存在的情况。
                .orElseGet(() -> ToolResult.failure(
                        "ORDER_NOT_FOUND",
                        "未找到订单",
                        List.of("订单 " + command.orderId() + " 不存在")
                ));
    }
}
