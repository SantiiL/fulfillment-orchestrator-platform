package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class TestOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> ordersById = new HashMap<>();
    private final List<Order> savedOrders = new ArrayList<>();
    private int saveCount;

    void store(Order order) {
        ordersById.put(order.getId(), order);
    }

    int saveCount() {
        return saveCount;
    }

    Order storedOrder(OrderId orderId) {
        return ordersById.get(orderId);
    }

    List<Order> savedOrders() {
        return savedOrders;
    }

    @Override
    public void save(Order order) {
        saveCount++;
        ordersById.put(order.getId(), order);
        savedOrders.add(order);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(ordersById.get(orderId));
    }

    @Override
    public long countAllocationsForFulfillmentNode(
            AssignedFulfillmentNodeId fulfillmentNodeId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    ) {
        return ordersById.values().stream()
                .filter(order -> fulfillmentNodeId.equals(order.getAssignedFulfillmentNodeId()))
                .filter(order -> order.getAllocatedAt() != null)
                .filter(order -> !order.getAllocatedAt().isBefore(startInclusive))
                .filter(order -> order.getAllocatedAt().isBefore(endExclusive))
                .count();
    }
}
