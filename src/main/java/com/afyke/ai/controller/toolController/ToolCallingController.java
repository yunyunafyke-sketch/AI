package com.afyke.ai.controller.toolController;

import com.afyke.ai.tool.DateTimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tool")
public class ToolCallingController {

    private final ChatClient chatClient;

    /**
     * application.properties
     * → Spring Boot 创建 OpenAiChatModel
     * → 创建 ChatClient.Builder
     * → Spring 把 Builder 注入构造方法
     * → builder.build() 创建 DefaultChatClient
     * → 保存到 chatClient 字段
     * @param builder
     */
    public ToolCallingController(ChatClient.Builder builder) {
        // build：使用 Spring Boot 已经配置好的 ChatModel 创建 ChatClient。
        // API Key、模型名和服务地址仍由 application.properties 提供。
        this.chatClient = builder.build();
    }

    @GetMapping("/time")
    public String queryCurrentTime(
            @RequestParam(defaultValue = "上海现在几点？请先使用工具查询，不要猜测。")
            String question) {

        return chatClient
                // prompt：开始组织本次模型请求。
                .prompt()
                // user：放入用户的自然语言问题。
                .user(question)
                // tools：只把这个工具对象暴露给本次请求。
                // Spring AI 会读取对象中带 @Tool 的方法并生成 Tool Definition。
                .tools(new DateTimeTools())
                // call：发起同步调用。
                // 如果模型返回 Tool Call，Spring AI 会在内部执行工具并再次请求模型。
                .call()
                // content：取出整个工具调用循环结束后的最终文本回答。
                .content();
    }
}