package com.santilugani.fulfillmentorchestrator.orders.application;

public interface GetOrderByIdUseCase {

    OrderResult getOrderById(GetOrderByIdQuery query);
}
