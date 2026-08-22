package com.afyke.ai.controller;

import com.afyke.ai.record.UserProfile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class AiController {

    private final ChatClient chatClient;

    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    @GetMapping("/test1")
    public String test1() {
        String answer = chatClient
                .prompt("Spring Boot 和 Spring AI 有什么关系？")
                .call()
                .content();
        System.out.println(answer);
        return answer;
    }

    @GetMapping("/test2")
    public String test2() {
        ChatClient teacherClient = chatClient.mutate()
                .defaultSystem("你是一名严谨的 Java 后端导师，请使用中文回答")
                .build();

        String answer = teacherClient
                .prompt("什么是 Spring IOC？")
                .call()
                .content();
        System.out.println(answer);
        return answer;
    }

    @GetMapping("/test3")
    public UserProfile test3() {
        UserProfile profile = chatClient
                .prompt()
                .user("生成一个用户资料，姓名叫张三，年龄 28 岁")
                .call()
                .entity(UserProfile.class);

        return profile;
    }


    //流式输出
    @GetMapping(value = "/test4", produces = MediaType.TEXT_EVENT_STREAM_VALUE+ ";charset=UTF-8")
    public Flux<String> stream() {
        return chatClient
                .prompt("请用三句话介绍 Java")
                .stream()
                .content();
    }

    @GetMapping("/ai")
    public String ask(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }


}
