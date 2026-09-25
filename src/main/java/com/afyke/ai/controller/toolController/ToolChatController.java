package com.afyke.ai.controller.toolController;

import com.afyke.ai.tool.OrderQueryTools;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tool-chat")
public class ToolChatController {

    private final ChatClient chatClient;
    private final OrderQueryTools orderQueryTools;

    public ToolChatController(
            ChatClient.Builder chatClientBuilder,
            OrderQueryTools orderQueryTools) {
        // build：根据 Spring AI 自动配置的模型创建 ChatClient。
        this.chatClient = chatClientBuilder.build();
        this.orderQueryTools = orderQueryTools;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String answer = chatClient.prompt()
                // system：要求实时订单信息必须通过 Tool 查询，不能猜测。
                .system("""
                        你是订单查询助手。
                        实时订单状态必须调用 queryOrderStatus 工具查询，不能根据常识猜测。
                        如果工具返回参数错误，请用中文告诉用户正确格式并要求补充。
                        如果订单不存在，请明确说明未找到，不要编造状态。
                        """)
                // user：本次用户的自然语言问题。
                .user(request.message())
                // tools：只给本次请求开放订单查询工具。
                .tools(orderQueryTools)
                // toolContext：把可信用户上下文直接传给 Tool，不让模型生成。
                // 这里只是学习用占位值；生产环境应从认证登录态读取。
                .toolContext(Map.of("userId", "demo-user"))
                // call：发起同步模型请求，并驱动 Tool Calling 循环。
                .call()
                // content：取得模型结合 Tool Result 生成的最终文字回答。
                .content();

        return new ChatResponse(answer);
    }

    public record ChatRequest(
            // 这里校验的是 HTTP 请求体，不等同于 Tool 参数校验。
            @NotBlank(message = "message 不能为空")
            String message
    ) {
    }

    public record ChatResponse(String answer) {
    }
}