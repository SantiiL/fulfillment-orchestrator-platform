package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;

import java.util.Objects;

public record CancelOrderCommand(OrderId orderId) {

    public CancelOrderCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
    }
}
