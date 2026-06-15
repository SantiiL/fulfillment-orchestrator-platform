package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;

public class OrderNotFoundException extends RuntimeException {

    private final OrderId orderId;

    public OrderNotFoundException(OrderId orderId) {
        super("Order was not found");
        this.orderId = orderId;
    }

    public OrderId getOrderId() {
        return orderId;
    }
}
