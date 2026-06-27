package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;

import java.util.Objects;

public record DispatchOrderCommand(OrderId orderId) {

    public DispatchOrderCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
    }
}
