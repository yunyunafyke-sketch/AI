package com.afyke.ai.order.repository;

import com.afyke.ai.order.domain.Order;

import java.util.Optional;

/**
 * 订单数据读取契约。
 * Service 只依赖这个接口，不关心数据来自内存、数据库还是远程 RPC。
 */
public interface OrderRepository {

    /**
     * findByOrderId：按照订单编号查询。
     * 找到时返回 Optional.of(order)，找不到时返回 Optional.empty()。
     */
    Optional<Order> findByOrderId(String orderId);
}