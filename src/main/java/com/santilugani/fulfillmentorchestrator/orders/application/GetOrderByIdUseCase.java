package com.santilugani.fulfillmentorchestrator.orders.application;

public interface GetOrderByIdUseCase {

    GetOrderByIdResult getOrderById(GetOrderByIdQuery query);
}
