package com.afyke.ai.order.service;

import java.time.LocalDateTime;

/**
 * 订单查询用的只读结果。
 * 只暴露模型回答用户问题真正需要的字段。
 */
public record OrderQueryResult(
        String orderId,
        String status,
        String statusDescription,
        LocalDateTime updatedAt
) {
}