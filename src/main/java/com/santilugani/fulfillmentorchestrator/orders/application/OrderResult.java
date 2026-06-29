package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;

import java.util.Objects;
import java.util.UUID;

public record OrderResult(
        UUID id,
        UUID sellerId,
        OrderStatus status,
        UUID assignedFulfillmentNodeId
) {

    public OrderResult {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sellerId, "sellerId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }

    public static OrderResult from(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        return new OrderResult(
                order.getId().value(),
                order.getSellerId().value(),
                order.getStatus(),
                order.getAssignedFulfillmentNodeId() == null ? null : order.getAssignedFulfillmentNodeId().value()
        );
    }
}
