package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import com.santilugani.fulfillmentorchestrator.orders.domain.SellerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetOrderByIdServiceTest {

    @Test
    void returnsOrderWhenItExists() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        GetOrderByIdService service = new GetOrderByIdService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(order);

        OrderResult result = service.getOrderById(new GetOrderByIdQuery(order.getId()));

        assertEquals(order.getId().value(), result.id());
        assertEquals(order.getSellerId().value(), result.sellerId());
        assertEquals(OrderStatus.CREATED, result.status());
        assertNull(result.assignedFulfillmentNodeId());
    }

    @Test
    void returnsAssignedFulfillmentNodeIdWhenOrderHasOne() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        GetOrderByIdService service = new GetOrderByIdService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        AssignedFulfillmentNodeId fulfillmentNodeId = new AssignedFulfillmentNodeId(UUID.randomUUID());
        order.allocate(fulfillmentNodeId);
        orderRepository.store(order);

        OrderResult result = service.getOrderById(new GetOrderByIdQuery(order.getId()));

        assertEquals(fulfillmentNodeId.value(), result.assignedFulfillmentNodeId());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        GetOrderByIdService service = new GetOrderByIdService(orderRepository);
        OrderId missingOrderId = OrderId.random();

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> service.getOrderById(new GetOrderByIdQuery(missingOrderId))
        );

        assertEquals(missingOrderId, exception.getOrderId());
        assertEquals("Order was not found", exception.getMessage());
    }
}
