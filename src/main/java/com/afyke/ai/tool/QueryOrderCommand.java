package com.afyke.ai.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Java 后端真正执行校验的命令对象。
 * 这里的注解负责约束输入，不只是给模型看的说明。
 */
public record QueryOrderCommand(
        // NotBlank：同时拦截 null、空字符串和全空格字符串。
        @NotBlank(message = "订单编号不能为空")

        // Pattern：只有满足正则表达式的订单号才能通过格式校验。
        @Pattern(
                regexp = "^A\\d{4,10}$",
                message = "订单编号格式错误，应为字母 A 加 4～10 位数字，例如 A1001"
        )
        String orderId
) {
}