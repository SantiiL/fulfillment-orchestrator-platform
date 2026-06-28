package com.santilugani.fulfillmentorchestrator.fulfillment.application;

public class FulfillmentNodeCodeAlreadyExistsException extends RuntimeException {

    private final String code;

    public FulfillmentNodeCodeAlreadyExistsException(String code) {
        super("Fulfillment node code already exists");
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
