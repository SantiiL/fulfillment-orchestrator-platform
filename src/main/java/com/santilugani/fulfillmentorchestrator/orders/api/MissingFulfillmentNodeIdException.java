package com.santilugani.fulfillmentorchestrator.orders.api;

public class MissingFulfillmentNodeIdException extends RuntimeException {

    public MissingFulfillmentNodeIdException() {
        super("fulfillmentNodeId is required");
    }
}
