package com.santilugani.fulfillmentorchestrator.orders.application;

public interface CancelOrderUseCase {

    CancelOrderResult cancelOrder(CancelOrderCommand command);
}
