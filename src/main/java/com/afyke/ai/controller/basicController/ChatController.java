package com.afyke.ai.controller.basicController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * 对外只暴露自己的聊天协议；前端不直接接触模型供应商地址和 API Key。
 */

/**
 * 这是实践，本章最重要的知识点之一，不能删
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    // SSE_TIMEOUT_MILLIS：SSE 异步请求最多保持 35 秒；35_000L 的单位是毫秒，L 表示 long 类型。
    // 它控制的是 Spring MVC 的 HTTP 异步请求，与下面 Reactor 的 timeout 属于两套不同的超时机制。
    private static final long SSE_TIMEOUT_MILLIS = 35_000L;

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        // 注入 4.3.1 中已配置好聊天记忆 Advisor 的 Bean。
        this.chatClient = chatClient;
    }

    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    /**
     * Controller 返回 SseEmitter 后，Spring 发现这是一个流式响应，于是保持 HTTP 连接打开，同时释放当前处理请求的线程。
     *
     * 普通接口是这样的：
     * 浏览器请求
     * → Controller 返回一个对象
     * → Spring 转成 JSON
     * → HTTP 请求结束
     /


     /**
     * SseEmitter 接口则是：
     * 浏览器请求
     * → Controller 返回 emitter
     * → Spring 保持 HTTP 连接打开
     * → 原来的请求线程被释放
     * → 后续通过 emitter.send(...) 多次发送数据
     * → emitter.complete() 后连接才结束
     */

    /**
     * 只有把 Flux 直接作为 Controller 返回值交给 Spring 时，Spring 才会替你订阅；你现在返回的是 SseEmitter，Flux 藏在方法内部，Spring 看不到它，所以代码才手动调用 subscribe()
     */
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        // 新对话由服务端生成 ID；真实项目应把 demo-user 改为登录态中的可信用户 ID。
        String conversationId = createOrReuseConversationId(request.conversationId());

        // SseEmitter：Spring MVC 用来维持 SSE 长连接的对象。Controller 返回它以后，这次 HTTP 请求不会立刻结束，
        // 服务端可以在 AI 生成回答的过程中，多次调用 emitter.send(...) 向浏览器推送文本片段。
        // new SseEmitter(timeout)：创建连接并设置异步请求超时时间；这里是 35 秒，不是 AI 模型本身的超时时间。
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        // prompt：开始组装一次 AI 请求，后面的 system、user、advisors 都是在配置这次请求。
        chatClient.prompt()
                // system：给 AI 设定身份、回答风格和长期规则，优先级通常高于本轮用户问题；业务规则不要由客户端传入。
                .system("你是企业售后与知识库 Agent 的学习版助手，请使用简洁、诚实的中文回答。")
                // user：放入用户这一轮真正提出的问题；这里只传本轮消息，历史消息由聊天记忆 Advisor 自动读取。
                .user(request.message())
                // advisors：类似 AI 调用的拦截器，可在请求发出前和响应回来后做额外处理；这里把会话 ID 交给聊天记忆 Advisor。
                // 相同 conversationId 会继续读取同一段聊天记录，不同 conversationId 的聊天记录彼此隔离。
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                // stream：使用流式方式调用 AI，让回答像打字一样分批返回，而不是等完整答案生成后一次返回。
                .stream()
                // content：只取出模型回答中的文字内容，得到会持续产生文本片段的 Flux<String>。
                .content()
                // timeout：订阅后 30 秒内没有首个信号，或相邻两个信号间隔超过 30 秒，就抛出超时错误。
                // 它限制的是“等待下一个信号的时间”，不是整轮模型回答的总耗时；超时后进入 subscribe 的 onError。
                .timeout(Duration.ofSeconds(30))
                // subscribe：订阅并正式启动这条流；依次定义“收到数据、发生错误、正常完成”三种处理方式。
                .subscribe(
                        // onNext/delta：把新增文字作为 SSE 事件发送，例如 event: delta、data: {"content":"你"}（实际还包含会话 ID 等字段）。
                        chunk -> send(emitter, "delta", new ChatEvent(conversationId, chunk, null, null)),
                        // onError：AI 调用、网络或超时发生异常时执行，而且执行后不会再调用下面的 onComplete。
                        error -> {
                            // 失败时给客户端可识别的事件，绝不伪造一段模型回答。
                            send(emitter, "error", new ChatEvent(
                                    conversationId,
                                    null,
                                    "AI_STREAM_FAILED",
                                    "AI 服务暂时不可用或响应超时，请稍后重试"
                            ));
                            emitter.complete();
                        },
                        // onComplete：模型所有文本片段都正常发送完毕后执行。
                        () -> {
                            // 模型正常结束；客户端收到 done 后结束本轮渲染。
                            send(emitter, "done", new ChatEvent(conversationId, null, null, null));
                            emitter.complete();
                        }
                );

        // onTimeout：注册超时回调；SSE 请求达到 35 秒时调用 complete() 结束连接。
        // emitter::complete 等价于 () -> emitter.complete()。
        emitter.onTimeout(emitter::complete);

        // onCompletion：注册请求结束后的回调，正常结束、超时或网络异常最终都会触发。
        // 当前 Lambda 内容为空，所以这里只是占位，实际没有执行清理逻辑。
        emitter.onCompletion(() -> { });
        return emitter;
    }

    private String createOrReuseConversationId(String requestedConversationId) {
        if (requestedConversationId == null || requestedConversationId.isBlank()) {
            return "demo-user:" + UUID.randomUUID();
        }
        return requestedConversationId;
    }

    /**
     * 向当前浏览器连接发送一条 SSE 事件。
     *
     * @param emitter 当前请求对应的 SSE 连接，用来把事件推送给客户端
     * @param eventName SSE 事件名称，例如 delta、error 或 done，客户端可根据名称分别处理
     * @param event 事件携带的业务数据，Spring 会把它序列化成 JSON
     */
    // private：这个发送方法只供当前 Controller 内部使用；void：方法只负责发送事件，不返回结果。
    private void send(SseEmitter emitter, String eventName, ChatEvent event) {
        try {
            // send：把构建完成的 SSE 事件写入当前 HTTP 长连接并推送给客户端。
            emitter.send(
                    // event：创建一条 SSE 事件的构建器，后面继续设置事件名称和数据。
                    SseEmitter.event()
                            // name：设置 SSE 的 event: 字段，例如 event: delta，方便客户端区分事件类型。
                            .name(eventName)
                            // data：设置 SSE 的 data: 字段；ChatEvent 会由 Spring 自动序列化成 JSON。
                            .data(event)
            );
        }
        catch (IOException exception) {
            // IOException：浏览器关闭页面、网络中断等情况可能导致事件无法写入客户端。
            // completeWithError：携带当前异常结束这条 SSE 连接，后面不再继续向该连接发送事件。
            emitter.completeWithError(exception);
        }
    }

    /** 客户端请求：新对话时 conversationId 可不传，message 不能为空。 */
    public record ChatRequest(String conversationId, @NotBlank String message) {
    }

    /** 每一条 SSE 的 JSON 数据结构。 */
    public record ChatEvent(String conversationId, String content, String code, String message) {
    }
}
