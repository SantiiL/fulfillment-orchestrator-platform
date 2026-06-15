package com.santilugani.fulfillmentorchestrator.orders.domain;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static OrderId random() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId from(String value) {
        return new OrderId(UUID.fromString(value));
    }
}
