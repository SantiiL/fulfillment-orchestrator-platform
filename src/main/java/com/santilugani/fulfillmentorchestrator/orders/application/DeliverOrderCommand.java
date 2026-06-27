package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;

import java.util.Objects;

public record DeliverOrderCommand(OrderId orderId) {

    public DeliverOrderCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
    }
}
