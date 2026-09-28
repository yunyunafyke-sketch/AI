package com.afyke.ai.order.domain;

import java.time.LocalDateTime;

/**
 * 应用内部使用的订单对象。
 * 当前只保留本篇查询需要的字段，后续可以由数据库实体或 RPC DTO 转换而来。
 */
public record Order(
        String orderId,
        OrderStatus status,
        LocalDateTime updatedAt
) {
}