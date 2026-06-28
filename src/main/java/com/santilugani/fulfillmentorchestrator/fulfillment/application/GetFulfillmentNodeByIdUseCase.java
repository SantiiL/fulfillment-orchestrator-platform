package com.santilugani.fulfillmentorchestrator.fulfillment.application;

public interface GetFulfillmentNodeByIdUseCase {

    FulfillmentNodeResult getFulfillmentNodeById(GetFulfillmentNodeByIdQuery query);
}
