package com.santilugani.fulfillmentorchestrator.orders.application;

public interface AllocateOrderUseCase {

    OrderResult allocateOrder(AllocateOrderCommand command);
}
