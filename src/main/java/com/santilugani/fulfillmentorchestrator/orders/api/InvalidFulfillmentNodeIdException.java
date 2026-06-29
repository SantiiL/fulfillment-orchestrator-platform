package com.santilugani.fulfillmentorchestrator.orders.api;

public class InvalidFulfillmentNodeIdException extends RuntimeException {

    public InvalidFulfillmentNodeIdException(String fulfillmentNodeId) {
        super("Fulfillment node id must be a valid UUID: %s".formatted(fulfillmentNodeId));
    }
}
