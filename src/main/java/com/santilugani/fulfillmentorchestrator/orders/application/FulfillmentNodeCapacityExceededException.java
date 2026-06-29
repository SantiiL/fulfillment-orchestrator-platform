package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;

public class FulfillmentNodeCapacityExceededException extends RuntimeException {

    private final AssignedFulfillmentNodeId fulfillmentNodeId;

    public FulfillmentNodeCapacityExceededException(AssignedFulfillmentNodeId fulfillmentNodeId) {
        super("Fulfillment node daily capacity has been reached");
        this.fulfillmentNodeId = fulfillmentNodeId;
    }

    public AssignedFulfillmentNodeId getFulfillmentNodeId() {
        return fulfillmentNodeId;
    }
}
