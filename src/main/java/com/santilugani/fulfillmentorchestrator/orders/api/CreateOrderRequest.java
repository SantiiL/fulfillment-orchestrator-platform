package com.santilugani.fulfillmentorchestrator.orders.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateOrderRequest(
        @NotBlank(message = "sellerId is required")
        @Pattern(
                regexp = CreateOrderRequest.UUID_PATTERN,
                message = "sellerId must be a valid UUID"
        )
        String sellerId
) {
    static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    public UUID sellerIdAsUuid() {
        return UUID.fromString(sellerId);
    }
}
