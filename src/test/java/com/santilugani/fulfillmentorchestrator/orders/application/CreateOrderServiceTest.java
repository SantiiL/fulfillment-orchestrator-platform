package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreateOrderServiceTest {

    @Test
    void createsOrderWithGeneratedIdAndCreatedStatus() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        CreateOrderService service = new CreateOrderService(orderRepository);
        UUID sellerId = UUID.randomUUID();

        OrderResult result = service.createOrder(new CreateOrderCommand(sellerId));

        assertNotNull(result.id());
        assertEquals(sellerId, result.sellerId());
        assertEquals(OrderStatus.CREATED, result.status());
        assertNull(result.assignedFulfillmentNodeId());
    }

    @Test
    void persistsCreatedOrderThroughRepository() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        CreateOrderService service = new CreateOrderService(orderRepository);
        UUID sellerId = UUID.randomUUID();

        OrderResult result = service.createOrder(new CreateOrderCommand(sellerId));

        assertEquals(1, orderRepository.savedOrders().size());

        Order savedOrder = orderRepository.savedOrders().getFirst();
        assertEquals(result.id(), savedOrder.getId().value());
        assertEquals(sellerId, savedOrder.getSellerId().value());
        assertEquals(OrderStatus.CREATED, savedOrder.getStatus());
        assertNull(savedOrder.getAssignedFulfillmentNodeId());
    }
}
