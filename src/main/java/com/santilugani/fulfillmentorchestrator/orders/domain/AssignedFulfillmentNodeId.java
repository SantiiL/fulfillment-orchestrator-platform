package com.santilugani.fulfillmentorchestrator.orders.domain;

import java.util.Objects;
import java.util.UUID;

public record AssignedFulfillmentNodeId(UUID value) {

    public AssignedFulfillmentNodeId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static AssignedFulfillmentNodeId from(String value) {
        return new AssignedFulfillmentNodeId(UUID.fromString(value));
    }
}
