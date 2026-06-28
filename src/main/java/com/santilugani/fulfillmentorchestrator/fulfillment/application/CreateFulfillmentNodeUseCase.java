package com.santilugani.fulfillmentorchestrator.fulfillment.application;

public interface CreateFulfillmentNodeUseCase {

    FulfillmentNodeResult createFulfillmentNode(CreateFulfillmentNodeCommand command);
}
