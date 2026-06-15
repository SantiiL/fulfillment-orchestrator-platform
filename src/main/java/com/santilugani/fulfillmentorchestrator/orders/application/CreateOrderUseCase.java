package com.santilugani.fulfillmentorchestrator.orders.application;

public interface CreateOrderUseCase {

    CreateOrderResult createOrder(CreateOrderCommand command);
}
