package com.santilugani.fulfillmentorchestrator.orders.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderLifecyclePolicy {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.ALLOCATED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.ALLOCATED, EnumSet.of(OrderStatus.READY_TO_SHIP, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.READY_TO_SHIP, EnumSet.of(OrderStatus.DISPATCHED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DISPATCHED, EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        Set<OrderStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        return allowedTargets.contains(targetStatus);
    }

    public void validateTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            throw new InvalidOrderStatusTransitionException(currentStatus, targetStatus);
        }
    }
}
