package com.santilugani.fulfillmentorchestrator.orders.application;

public interface DispatchOrderUseCase {

    OrderResult dispatchOrder(DispatchOrderCommand command);
}
