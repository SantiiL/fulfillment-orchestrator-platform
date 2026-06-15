package com.santilugani.fulfillmentorchestrator.orders.api;

public class InvalidOrderIdException extends RuntimeException {

    public InvalidOrderIdException(String orderId) {
        super("Order id must be a valid UUID: %s".formatted(orderId));
    }
}
