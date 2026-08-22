package com.afyke.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MemoryAIController {
    private final ChatClient chatClient;

    public MemoryAIController(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

//    @GetMapping(value = "/test5")
//    public String stream() {
//        String answer = chatClient.prompt()
//                .advisors(advisor -> advisor
//                        .param(ChatMemory.CONVERSATION_ID, "conversation-001"))
//                .user("我刚才说的项目叫什么？")
//                .call()
//                .content();
//        return answer;
//    }

}
