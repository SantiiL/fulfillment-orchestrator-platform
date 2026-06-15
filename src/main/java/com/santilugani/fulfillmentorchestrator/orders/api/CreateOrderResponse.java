package com.santilugani.fulfillmentorchestrator.orders.api;

import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;

import java.util.UUID;

public record CreateOrderResponse(UUID id, UUID sellerId, OrderStatus status) {
}
