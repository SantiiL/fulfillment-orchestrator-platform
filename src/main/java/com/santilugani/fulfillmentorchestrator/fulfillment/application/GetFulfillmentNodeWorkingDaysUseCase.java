package com.santilugani.fulfillmentorchestrator.fulfillment.application;

public interface GetFulfillmentNodeWorkingDaysUseCase {

    FulfillmentNodeWorkingDaysResult getWorkingDays(GetFulfillmentNodeWorkingDaysQuery query);
}
