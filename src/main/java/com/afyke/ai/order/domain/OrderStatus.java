package com.afyke.ai.order.domain;

/**
 * 订单在当前学习项目中可能出现的状态。
 * code 使用稳定英文值，description 用于给用户和模型解释。
 */
public enum OrderStatus {

    PAID("订单已付款，等待发货"),
    PROCESSING("订单正在处理中"),
    SHIPPED("订单已发货"),
    COMPLETED("订单已完成"),
    CANCELLED("订单已取消");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}