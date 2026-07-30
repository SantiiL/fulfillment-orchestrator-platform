package com.santilugani.fulfillmentorchestrator.fulfillment.api;

public class InvalidFulfillmentNodeWorkingDaysRequestException extends RuntimeException {

    public InvalidFulfillmentNodeWorkingDaysRequestException() {
        super("workingDays must contain at least one valid day of week");
    }
}
