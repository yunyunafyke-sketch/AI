package com.afyke.ai.controller;

import com.afyke.ai.record.AfterSaleRequest;
import com.afyke.ai.record.CustomerIntent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JSONSchemaCrontroller {
    private ChatClient chatClient;
    public JSONSchemaCrontroller(ChatClient.Builder builder) {
        this.chatClient= builder.build();
    }


    @GetMapping("/test5")
    public CustomerIntent getJSONSchemaCrontroller() {
        CustomerIntent result = chatClient
                .prompt()
                .system("""
                你是企业售后系统的意图识别助手。
                只做意图识别，不查询订单，不修改数据，不创建工单。
                intent 只能是 ORDER_STATUS、LOGISTICS、INVENTORY、AFTER_SALE、POLICY、OTHER。
                """)
                .user("请识别用户问题：我的订单什么时候发货？")
                .call()
                .entity(CustomerIntent.class);
        return result;
    }

    @GetMapping("/test6")
    public AfterSaleRequest test6() {
        AfterSaleRequest result = chatClient
                .prompt()
                .system("""
                你是售后信息抽取助手。
                只从用户输入中提取已有信息，不要猜测不存在的订单号。
                没有订单号时，orderNo 返回空字符串。
                """)
                .user("用户输入：订单 A20260821001 收到的商品有破损，我想申请售后。")
                .call()
                .entity(AfterSaleRequest.class);
        return result;
    }


}
