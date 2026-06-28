package com.santilugani.fulfillmentorchestrator.fulfillment.domain;

import java.util.Objects;
import java.util.UUID;

public record FulfillmentNodeId(UUID value) {

    public FulfillmentNodeId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static FulfillmentNodeId random() {
        return new FulfillmentNodeId(UUID.randomUUID());
    }

    public static FulfillmentNodeId from(String value) {
        return new FulfillmentNodeId(UUID.fromString(value));
    }
}
