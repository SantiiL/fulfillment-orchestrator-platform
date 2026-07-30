package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;

import java.util.Objects;

public record GetFulfillmentNodeWorkingDaysQuery(FulfillmentNodeId fulfillmentNodeId) {

    public GetFulfillmentNodeWorkingDaysQuery {
        Objects.requireNonNull(fulfillmentNodeId, "fulfillmentNodeId must not be null");
    }
}
