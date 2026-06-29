package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;

public class FulfillmentNodeInactiveException extends RuntimeException {

    private final AssignedFulfillmentNodeId fulfillmentNodeId;

    public FulfillmentNodeInactiveException(AssignedFulfillmentNodeId fulfillmentNodeId) {
        super("Fulfillment node is inactive");
        this.fulfillmentNodeId = fulfillmentNodeId;
    }

    public AssignedFulfillmentNodeId getFulfillmentNodeId() {
        return fulfillmentNodeId;
    }
}
