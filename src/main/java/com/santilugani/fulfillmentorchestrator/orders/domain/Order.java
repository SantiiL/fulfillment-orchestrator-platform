package com.santilugani.fulfillmentorchestrator.orders.domain;

import java.util.Objects;

public final class Order {

    private static final OrderLifecyclePolicy LIFECYCLE_POLICY = new OrderLifecyclePolicy();

    private final OrderId id;
    private final SellerId sellerId;
    private OrderStatus status;

    public Order(OrderId id, SellerId sellerId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
        this.status = OrderStatus.CREATED;
    }

    public OrderId getId() {
        return id;
    }

    public SellerId getSellerId() {
        return sellerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void allocate() {
        transitionTo(OrderStatus.ALLOCATED);
    }

    public void markReadyToShip() {
        transitionTo(OrderStatus.READY_TO_SHIP);
    }

    public void dispatch() {
        transitionTo(OrderStatus.DISPATCHED);
    }

    public void deliver() {
        transitionTo(OrderStatus.DELIVERED);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    private void transitionTo(OrderStatus targetStatus) {
        LIFECYCLE_POLICY.validateTransition(status, targetStatus);
        this.status = targetStatus;
    }
}
