package com.afyke.ai.tool;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OrderQueryTools {

    private final Validator validator;

    /**
     * 第 13 天只用内存数据验证参数流程。
     * 第 14 天将这里替换为真正的 OrderService 或下游 RPC。
     */
    private final Map<String, OrderStatusData> mockOrders = Map.of(
            "A1001", new OrderStatusData("A1001", "SHIPPED", "订单已发货"),
            "A1002", new OrderStatusData("A1002", "PAID", "订单已付款，等待发货")
    );

    public OrderQueryTools(Validator validator) {
        // Validator 由 Spring Boot Validation 自动配置并注入。
        this.validator = validator;
    }

    @Tool(
            name = "queryOrderStatus",
            description = "根据订单编号查询当前订单状态；用户询问订单是否付款、发货或完成时使用；只读，不修改订单"
    )
    public ToolResult<OrderStatusData> queryOrderStatus(
            @ToolParam(
                    description = "订单编号，格式为大写字母 A 加 4～10 位数字，例如 A1001",
                    required = true
            )
            String orderId,
            ToolContext toolContext) {

        // ToolContext 中的数据由 Java 应用提供，而不是由模型生成。
        Object userIdValue = toolContext.getContext().get("userId");
        String userId = userIdValue == null ? "" : userIdValue.toString();

        if (userId.isBlank()) {
            // 缺少可信调用上下文时拒绝继续，不让模型自己补 userId。
            return ToolResult.failure(
                    "CONTEXT_MISSING",
                    "缺少调用用户上下文",
                    List.of("Java 应用必须通过 ToolContext 提供 userId")
            );
        }

        // 把模型参数放进受 Bean Validation 约束的命令对象。
        return queryOrderStatusInternal(orderId, userId);
    }

    /**
     * 内部方法把校验流程与 Spring AI 调用入口分开，便于做确定性的单元测试。
     */
    ToolResult<OrderStatusData> queryOrderStatusInternal(
            String orderId,
            String userId) {

        QueryOrderCommand command = new QueryOrderCommand(orderId);

        // validate：执行 @NotBlank、@Pattern 等 Bean Validation 约束。
        var violations = validator.validate(command);

        if (!violations.isEmpty()) {
            List<String> errors = violations.stream()
                    // getMessage：只提取受控的校验提示，不返回内部堆栈。
                    .map(ConstraintViolation::getMessage)
                    // sorted：保证错误顺序稳定，方便测试和日志比较。
                    .sorted()
                    // toList：收集为不可变列表。
                    .toList();

            // 参数校验失败后立即返回，不查询真实业务数据。
            return ToolResult.failure(
                    "INVALID_ARGUMENT",
                    "工具参数校验失败",
                    errors
            );
        }

        OrderStatusData order = mockOrders.get(command.orderId());

        if (order == null) {
            // 格式正确但数据不存在，属于业务校验失败。
            return ToolResult.failure(
                    "ORDER_NOT_FOUND",
                    "未找到订单",
                    List.of("订单 " + command.orderId() + " 不存在")
            );
        }

        // userId 当前只证明可信上下文已经进入 Tool。
        // 第 19 天应在这里或业务 Service 中继续校验用户是否有权查看该订单。
        return ToolResult.success(
                "用户 " + userId + " 的订单状态查询成功",
                order
        );
    }

    /**
     * 返回给模型的订单数据只保留必要字段，不直接返回数据库实体。
     */
    public record OrderStatusData(
            String orderId,
            String status,
            String description
    ) {
    }
}