package com.santilugani.fulfillmentorchestrator.orders.domain;

public enum OrderStatus {
    CREATED,
    ALLOCATED,
    READY_TO_SHIP,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}
