package com.afyke.ai.config;

import java.time.Duration;

import redis.clients.jedis.RedisClient;
import org.springframework.beans.factory.annotation.Value;
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

    /** Redis 服务地址。 */
    @Value("${spring.ai.chat.memory.repository.redis.host:localhost}")
    private String redisHost;

    /** Redis 服务端口。 */
    @Value("${spring.ai.chat.memory.repository.redis.port:6379}")
    private int redisPort;

    /** 聊天消息在 Redis 中的键前缀。 */
    @Value("${spring.ai.chat.memory.repository.redis.key-prefix:chat-memory:}")
    private String redisKeyPrefix;

    /** 聊天消息过期时间。 */
    @Value("${spring.ai.chat.memory.repository.redis.time-to-live:24h}")
    private Duration redisTimeToLive;

    /**
     * 创建 Redis Stack 客户端，供聊天记忆仓库读写会话消息。
     */
    @Bean
    RedisClient redisClient() {
        return RedisClient.builder().hostAndPort(redisHost, redisPort).build();
    }

    /**
     * 显式创建 Redis 聊天记忆仓库，避免自定义 ChatMemory Bean 使自动配置退让。
     * @param redisClient Redis Stack 客户端
     * @return Redis 聊天记忆仓库
     */
    @Bean
    RedisChatMemoryRepository redisChatMemoryRepository(RedisClient redisClient) {
        return RedisChatMemoryRepository.builder()
                // 复用当前配置创建的客户端，并保持教程中的键前缀和过期策略。
                .jedisClient(redisClient)
                .keyPrefix(redisKeyPrefix)
                .timeToLive(redisTimeToLive)
                .build();
    }

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
