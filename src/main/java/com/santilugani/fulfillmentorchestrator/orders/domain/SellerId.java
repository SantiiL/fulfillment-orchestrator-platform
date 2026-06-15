package com.santilugani.fulfillmentorchestrator.orders.domain;

import java.util.Objects;
import java.util.UUID;

public record SellerId(UUID value) {

    public SellerId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static SellerId random() {
        return new SellerId(UUID.randomUUID());
    }

    public static SellerId from(String value) {
        return new SellerId(UUID.fromString(value));
    }
}
