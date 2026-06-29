package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId orderId);

    long countAllocationsForFulfillmentNode(
            AssignedFulfillmentNodeId fulfillmentNodeId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    );
}
