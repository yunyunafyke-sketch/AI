package com.afyke.ai.order.service;

import com.afyke.ai.order.domain.Order;
import com.afyke.ai.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 订单查询业务层。
 * 它不依赖 ChatClient、Prompt 或 @Tool，因此可以被多种入口复用。
 */
@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        // Spring 会注入当前唯一的 InMemoryOrderRepository 实现。
        this.orderRepository = orderRepository;
    }

    /**
     * 根据订单号查询订单状态。
     *
     * Optional 可以理解成一个“可能装有结果，也可能是空的盒子”：
     * - 找到订单：返回 Optional.of(OrderQueryResult)，盒子里有查询结果；
     * - 没找到订单：返回 Optional.empty()，盒子为空。
     *
     * 使用 Optional 的目的，是明确提醒调用方“订单可能不存在”，
     * 避免直接返回 null 后，调用方忘记判空而产生 NullPointerException。
     */
    public Optional<OrderQueryResult> queryOrderStatus(String orderId) {
        // findByOrderId 的返回值本身就是 Optional<Order>：
        // 找到订单时里面装着 Order，找不到时是 Optional.empty()。
        return orderRepository.findByOrderId(orderId)
                // map：只有 Optional 中存在 Order 时，才调用 toQueryResult 进行转换。
                // 如果原来是 Optional.empty()，map 不会执行，结果仍然是 Optional.empty()。
                .map(this::toQueryResult);
    }

    private OrderQueryResult toQueryResult(Order order) {
        return new OrderQueryResult(
                order.orderId(),
                order.status().name(),
                order.status().getDescription(),
                order.updatedAt()
        );
    }
}
