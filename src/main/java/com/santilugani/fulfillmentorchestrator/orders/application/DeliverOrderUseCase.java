package com.santilugani.fulfillmentorchestrator.orders.application;

public interface DeliverOrderUseCase {

    OrderResult deliverOrder(DeliverOrderCommand command);
}
