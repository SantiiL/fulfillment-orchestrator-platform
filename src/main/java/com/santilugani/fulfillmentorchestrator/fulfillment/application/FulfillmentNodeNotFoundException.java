package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;

public class FulfillmentNodeNotFoundException extends RuntimeException {

    private final FulfillmentNodeId fulfillmentNodeId;

    public FulfillmentNodeNotFoundException(FulfillmentNodeId fulfillmentNodeId) {
        super("Fulfillment node was not found");
        this.fulfillmentNodeId = fulfillmentNodeId;
    }

    public FulfillmentNodeId getFulfillmentNodeId() {
        return fulfillmentNodeId;
    }
}
