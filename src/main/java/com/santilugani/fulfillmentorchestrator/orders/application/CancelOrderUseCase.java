package com.santilugani.fulfillmentorchestrator.orders.application;

public interface CancelOrderUseCase {

    OrderResult cancelOrder(CancelOrderCommand command);
}
