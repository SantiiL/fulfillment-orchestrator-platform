package com.santilugani.fulfillmentorchestrator.fulfillment.api;

public class InvalidFulfillmentNodeIdException extends RuntimeException {

    private final String invalidValue;

    public InvalidFulfillmentNodeIdException(String invalidValue) {
        super("Fulfillment node id must be a valid UUID");
        this.invalidValue = invalidValue;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}
