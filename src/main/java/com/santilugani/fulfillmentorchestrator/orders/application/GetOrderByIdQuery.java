package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;

import java.util.Objects;

public record GetOrderByIdQuery(OrderId orderId) {

    public GetOrderByIdQuery {
        Objects.requireNonNull(orderId, "orderId must not be null");
    }
}
