package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import java.util.Objects;

public record CreateFulfillmentNodeCommand(String code, String name, int maxDailyCapacity) {

    public CreateFulfillmentNodeCommand {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
