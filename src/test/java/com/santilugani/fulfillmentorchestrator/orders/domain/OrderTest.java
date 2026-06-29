package com.santilugani.fulfillmentorchestrator.orders.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void createsOrderInCreatedStatus() {
        OrderId orderId = OrderId.random();
        SellerId sellerId = SellerId.random();

        Order order = new Order(orderId, sellerId);

        assertEquals(orderId, order.getId());
        assertEquals(sellerId, order.getSellerId());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertNull(order.getAssignedFulfillmentNodeId());
    }

    @Test
    void allocatesOrderFromCreatedAndAssignsFulfillmentNode() {
        Order order = new Order(OrderId.random(), SellerId.random());
        AssignedFulfillmentNodeId fulfillmentNodeId = assignedFulfillmentNodeId();

        order.allocate(fulfillmentNodeId);

        assertEquals(OrderStatus.ALLOCATED, order.getStatus());
        assertEquals(fulfillmentNodeId, order.getAssignedFulfillmentNodeId());
    }

    @Test
    void rejectsAllocationWithoutFulfillmentNodeId() {
        Order order = new Order(OrderId.random(), SellerId.random());

        NullPointerException exception = assertThrows(NullPointerException.class, () -> order.allocate(null));

        assertEquals("assignedFulfillmentNodeId must not be null", exception.getMessage());
    }

    @Test
    void cancelsOrderFromCreated() {
        Order order = new Order(OrderId.random(), SellerId.random());

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void marksOrderReadyToShipFromAllocated() {
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(assignedFulfillmentNodeId());

        order.markReadyToShip();

        assertEquals(OrderStatus.READY_TO_SHIP, order.getStatus());
    }

    @Test
    void cancelsOrderFromAllocated() {
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(assignedFulfillmentNodeId());

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void dispatchesOrderFromReadyToShip() {
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(assignedFulfillmentNodeId());
        order.markReadyToShip();

        order.dispatch();

        assertEquals(OrderStatus.DISPATCHED, order.getStatus());
    }

    @Test
    void cancelsOrderFromReadyToShip() {
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(assignedFulfillmentNodeId());
        order.markReadyToShip();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void deliversOrderFromDispatched() {
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(assignedFulfillmentNodeId());
        order.markReadyToShip();
        order.dispatch();

        order.deliver();

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void rejectsInvalidTransitionThroughBehavior() {
        Order order = new Order(OrderId.random(), SellerId.random());

        InvalidOrderStatusTransitionException exception =
                assertThrows(InvalidOrderStatusTransitionException.class, order::deliver);

        assertEquals(OrderStatus.CREATED, exception.getCurrentStatus());
        assertEquals(OrderStatus.DELIVERED, exception.getTargetStatus());
    }

    @Test
    void deliveredOrderCannotTransitionAgain() {
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(assignedFulfillmentNodeId());
        order.markReadyToShip();
        order.dispatch();
        order.deliver();

        assertThrows(InvalidOrderStatusTransitionException.class, order::cancel);
    }

    @Test
    void cancelledOrderCannotTransitionAgain() {
        Order order = new Order(OrderId.random(), SellerId.random());
        order.cancel();

        assertThrows(InvalidOrderStatusTransitionException.class, () -> order.allocate(assignedFulfillmentNodeId()));
    }

    private AssignedFulfillmentNodeId assignedFulfillmentNodeId() {
        return new AssignedFulfillmentNodeId(UUID.randomUUID());
    }
}
