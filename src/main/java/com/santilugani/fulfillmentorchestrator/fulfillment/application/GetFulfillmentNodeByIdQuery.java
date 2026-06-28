package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;

import java.util.Objects;

public record GetFulfillmentNodeByIdQuery(FulfillmentNodeId fulfillmentNodeId) {

    public GetFulfillmentNodeByIdQuery {
        Objects.requireNonNull(fulfillmentNodeId, "fulfillmentNodeId must not be null");
    }
}
