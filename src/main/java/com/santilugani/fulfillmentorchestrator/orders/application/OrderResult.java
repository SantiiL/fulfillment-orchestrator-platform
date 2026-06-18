package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;

import java.util.Objects;
import java.util.UUID;

public record OrderResult(UUID id, UUID sellerId, OrderStatus status) {

    public OrderResult {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sellerId, "sellerId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
