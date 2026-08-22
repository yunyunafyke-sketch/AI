package com.afyke.ai.config;

// 调用大模型的高级客户端。
import org.springframework.ai.chat.client.ChatClient;
// 负责在模型调用前后自动处理聊天记忆。
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
// 聊天记忆的统一类型。
import org.springframework.ai.chat.memory.ChatMemory;
// 只保留最近若干条消息的滑动窗口实现。
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
// 真正将消息读写到 Redis Stack 的实现。
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 告诉 Spring：这是配置类，启动时读取其中的 @Bean 方法。
@Configuration
public class ChatMemoryConfig {

    // 创建“聊天记忆”对象，并交给 Spring 管理。
    @Bean
    ChatMemory chatMemory(RedisChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                // 指定聊天消息存入 Redis Stack，后续也从 Redis 读取。
                .chatMemoryRepository(repository)
                // 最多保留最近 10 条消息给模型参考；约等于 5 次问答。
                .maxMessages(10)
                .build();
    }

    // 创建业务代码实际使用的 ChatClient，并交给 Spring 管理。
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                // 注册聊天记忆顾问：调用前读历史，调用后保存本轮问答。
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}