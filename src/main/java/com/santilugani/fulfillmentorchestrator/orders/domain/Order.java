package com.santilugani.fulfillmentorchestrator.orders.domain;

import java.util.Objects;

public final class Order {

    private static final OrderLifecyclePolicy LIFECYCLE_POLICY = new OrderLifecyclePolicy();

    private final OrderId id;
    private final SellerId sellerId;
    private OrderStatus status;
    private AssignedFulfillmentNodeId assignedFulfillmentNodeId;

    public Order(OrderId id, SellerId sellerId) {
        this(id, sellerId, OrderStatus.CREATED, null);
    }

    public static Order reconstitute(OrderId id, SellerId sellerId, OrderStatus status) {
        return reconstitute(id, sellerId, status, null);
    }

    public static Order reconstitute(
            OrderId id,
            SellerId sellerId,
            OrderStatus status,
            AssignedFulfillmentNodeId assignedFulfillmentNodeId
    ) {
        return new Order(id, sellerId, status, assignedFulfillmentNodeId);
    }

    private Order(
            OrderId id,
            SellerId sellerId,
            OrderStatus status,
            AssignedFulfillmentNodeId assignedFulfillmentNodeId
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.assignedFulfillmentNodeId = assignedFulfillmentNodeId;
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

    public AssignedFulfillmentNodeId getAssignedFulfillmentNodeId() {
        return assignedFulfillmentNodeId;
    }

    public void allocate(AssignedFulfillmentNodeId assignedFulfillmentNodeId) {
        Objects.requireNonNull(assignedFulfillmentNodeId, "assignedFulfillmentNodeId must not be null");
        transitionTo(OrderStatus.ALLOCATED);
        this.assignedFulfillmentNodeId = assignedFulfillmentNodeId;
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
