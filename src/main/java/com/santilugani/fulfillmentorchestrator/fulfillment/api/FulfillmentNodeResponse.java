package com.santilugani.fulfillmentorchestrator.fulfillment.api;

import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeResult;

import java.util.Objects;
import java.util.UUID;

public record FulfillmentNodeResponse(
        UUID id,
        String code,
        String name,
        int maxDailyCapacity,
        boolean active
) {

    public FulfillmentNodeResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }

    public static FulfillmentNodeResponse from(FulfillmentNodeResult result) {
        Objects.requireNonNull(result, "result must not be null");

        return new FulfillmentNodeResponse(
                result.id(),
                result.code(),
                result.name(),
                result.maxDailyCapacity(),
                result.active()
        );
    }
}
