package com.santilugani.fulfillmentorchestrator.orders.application;

public interface CreateOrderUseCase {

    OrderResult createOrder(CreateOrderCommand command);
}
