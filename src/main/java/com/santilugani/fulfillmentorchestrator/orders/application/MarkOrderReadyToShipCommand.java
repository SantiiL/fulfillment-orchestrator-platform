package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;

import java.util.Objects;

public record MarkOrderReadyToShipCommand(OrderId orderId) {

    public MarkOrderReadyToShipCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
    }
}
