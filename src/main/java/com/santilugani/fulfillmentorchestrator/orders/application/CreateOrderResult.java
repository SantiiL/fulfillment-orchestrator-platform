package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;

import java.util.Objects;
import java.util.UUID;

public record CreateOrderResult(UUID id, UUID sellerId, OrderStatus status) {

    public CreateOrderResult {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sellerId, "sellerId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
