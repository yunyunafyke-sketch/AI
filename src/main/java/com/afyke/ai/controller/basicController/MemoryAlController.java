package com.afyke.ai.controller.basicController;

// 校验 @RequestBody 中的字段规则。
import jakarta.validation.Valid;
// 声明字符串不能为空、不能只包含空格。
import jakarta.validation.constraints.NotBlank;
// 调用大模型的高级客户端。
import org.springframework.ai.chat.client.ChatClient;
// 读取 Spring AI 规定的会话 ID 参数名。
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// 声明这是 REST Controller，方法返回值会自动转成 JSON。
@RestController
// 为本类所有接口统一加上 /api/chat 路径前缀。
@RequestMapping("/api/chat")
public class MemoryAlController {

    // 复用已注册聊天记忆 Advisor 的 ChatClient。
    private final ChatClient chatClient;

    // Spring 自动注入配置类创建的 ChatClient，无须手动 new。
    public MemoryAlController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // 接收 POST /api/chat 请求。
    @PostMapping
    // 正常处理时返回 HTTP 200。
    @ResponseStatus(HttpStatus.OK)
    // @Valid 启用字段校验；@RequestBody 将请求 JSON 转为 ChatRequest。
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String requestedConversationId = request.conversationId();
        final String conversationId;
        if (requestedConversationId == null || requestedConversationId.isBlank()) {
            // 新对话：生成随机 ID。实际项目应把 demo-user 替换为登录态中的用户 ID。
            conversationId = "demo-user:" + UUID.randomUUID();
        }else {
            conversationId = requestedConversationId;
        }

        // 发起本轮模型调用；记忆 Advisor 会在调用前后自动处理 Redis 中的聊天记录。
        // prompt：开始组装一次 AI 请求，后面的 user、advisors 都是在配置这次请求。
        String answer = chatClient.prompt()
                // user：放入用户这一轮真正提出的问题；这里只传本轮消息，历史消息由聊天记忆 Advisor 自动读取。
                .user(request.message())
                // advisors：类似 AI 调用的拦截器，可在请求发出前和响应回来后做额外处理；这里把会话 ID 交给聊天记忆 Advisor。
                // 相同 conversationId 会继续读取同一段聊天记录，不同 conversationId 的聊天记录彼此隔离。
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                // call：使用同步方式执行 AI 请求，当前线程会等待模型生成完整响应。
                .call()
                // content：从完整的模型响应对象中只取出文字回答。
                .content();

        // 把会话 ID 和回答一起返回；客户端必须保存 ID，下一次继续携带它。
        return new ChatResponse(conversationId, answer);
    }

    // 请求 JSON：conversationId 可省略（表示新建对话），message 必须非空。
    public record ChatRequest(String conversationId, @NotBlank String message) {
    }

    // 响应 JSON：返回本次会话 ID 和模型回答。
    public record ChatResponse(String conversationId, String answer) {
    }
}
