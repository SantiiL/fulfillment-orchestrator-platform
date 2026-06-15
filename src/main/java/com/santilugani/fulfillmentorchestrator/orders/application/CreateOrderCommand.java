package com.santilugani.fulfillmentorchestrator.orders.application;

import java.util.Objects;
import java.util.UUID;

public record CreateOrderCommand(UUID sellerId) {

    public CreateOrderCommand {
        Objects.requireNonNull(sellerId, "sellerId must not be null");
    }
}
