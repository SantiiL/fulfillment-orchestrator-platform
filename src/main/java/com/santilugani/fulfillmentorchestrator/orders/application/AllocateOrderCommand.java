package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;

import java.util.Objects;

public record AllocateOrderCommand(OrderId orderId, AssignedFulfillmentNodeId fulfillmentNodeId) {

    public AllocateOrderCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(fulfillmentNodeId, "fulfillmentNodeId must not be null");
    }
}
