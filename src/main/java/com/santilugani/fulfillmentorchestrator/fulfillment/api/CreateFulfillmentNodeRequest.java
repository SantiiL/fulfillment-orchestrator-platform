package com.santilugani.fulfillmentorchestrator.fulfillment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateFulfillmentNodeRequest(
        @NotBlank(message = "code is required")
        String code,
        @NotBlank(message = "name is required")
        String name,
        @Positive(message = "maxDailyCapacity must be positive")
        int maxDailyCapacity
) {
}
