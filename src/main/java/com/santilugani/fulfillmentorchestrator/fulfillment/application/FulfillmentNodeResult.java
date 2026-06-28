package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;

import java.util.Objects;
import java.util.UUID;

public record FulfillmentNodeResult(
        UUID id,
        String code,
        String name,
        int maxDailyCapacity,
        boolean active
) {

    public FulfillmentNodeResult {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(name, "name must not be null");

        if (maxDailyCapacity <= 0) {
            throw new IllegalArgumentException("maxDailyCapacity must be positive");
        }
    }

    public static FulfillmentNodeResult from(FulfillmentNode fulfillmentNode) {
        Objects.requireNonNull(fulfillmentNode, "fulfillmentNode must not be null");

        return new FulfillmentNodeResult(
                fulfillmentNode.getId().value(),
                fulfillmentNode.getCode(),
                fulfillmentNode.getName(),
                fulfillmentNode.getMaxDailyCapacity(),
                fulfillmentNode.isActive()
        );
    }
}
