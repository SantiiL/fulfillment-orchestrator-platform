package com.santilugani.fulfillmentorchestrator.orders.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

public final class Order {

    private static final OrderLifecyclePolicy LIFECYCLE_POLICY = new OrderLifecyclePolicy();

    private final OrderId id;
    private final SellerId sellerId;
    private OrderStatus status;
    private AssignedFulfillmentNodeId assignedFulfillmentNodeId;
    private OffsetDateTime allocatedAt;

    public Order(OrderId id, SellerId sellerId) {
        this(id, sellerId, OrderStatus.CREATED, null, null);
    }

    public static Order reconstitute(OrderId id, SellerId sellerId, OrderStatus status) {
        return reconstitute(id, sellerId, status, null, null);
    }

    public static Order reconstitute(
            OrderId id,
            SellerId sellerId,
            OrderStatus status,
            AssignedFulfillmentNodeId assignedFulfillmentNodeId
    ) {
        return reconstitute(id, sellerId, status, assignedFulfillmentNodeId, null);
    }

    public static Order reconstitute(
            OrderId id,
            SellerId sellerId,
            OrderStatus status,
            AssignedFulfillmentNodeId assignedFulfillmentNodeId,
            OffsetDateTime allocatedAt
    ) {
        return new Order(id, sellerId, status, assignedFulfillmentNodeId, allocatedAt);
    }

    private Order(
            OrderId id,
            SellerId sellerId,
            OrderStatus status,
            AssignedFulfillmentNodeId assignedFulfillmentNodeId,
            OffsetDateTime allocatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.assignedFulfillmentNodeId = assignedFulfillmentNodeId;
        this.allocatedAt = allocatedAt;
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

    public OffsetDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void validateCanAllocate() {
        LIFECYCLE_POLICY.validateTransition(status, OrderStatus.ALLOCATED);
    }

    public void allocate(AssignedFulfillmentNodeId assignedFulfillmentNodeId, OffsetDateTime allocatedAt) {
        Objects.requireNonNull(assignedFulfillmentNodeId, "assignedFulfillmentNodeId must not be null");
        Objects.requireNonNull(allocatedAt, "allocatedAt must not be null");
        validateCanAllocate();
        this.status = OrderStatus.ALLOCATED;
        this.assignedFulfillmentNodeId = assignedFulfillmentNodeId;
        this.allocatedAt = allocatedAt;
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
