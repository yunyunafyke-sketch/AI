package com.afyke.ai.order.repository;

import com.afyke.ai.order.domain.Order;
import com.afyke.ai.order.domain.OrderStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 第 14 天使用的本地数据源。
 * 它让完整调用链无需数据库即可运行；以后接 PostgreSQL 或 RPC 时替换此实现。
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = Map.of(
            "A1001", new Order(
                    "A1001",
                    OrderStatus.SHIPPED,
                    LocalDateTime.of(2026, 9, 25, 10, 30)
            ),
            "A1002", new Order(
                    "A1002",
                    OrderStatus.PAID,
                    LocalDateTime.of(2026, 9, 25, 11, 15)
            ),
            "A1003", new Order(
                    "A1003",
                    OrderStatus.COMPLETED,
                    LocalDateTime.of(2026, 9, 24, 18, 0)
            )
    );

    @Override
    public Optional<Order> findByOrderId(String orderId) {
        // ofNullable：Map 找不到数据时得到 Optional.empty()，而不是返回 null。
        return Optional.ofNullable(orders.get(orderId));
    }
}