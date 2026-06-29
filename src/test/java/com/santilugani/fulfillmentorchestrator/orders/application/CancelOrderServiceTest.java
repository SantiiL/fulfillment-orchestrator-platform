package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.InvalidOrderStatusTransitionException;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import com.santilugani.fulfillmentorchestrator.orders.domain.SellerId;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelOrderServiceTest {

    @Test
    void cancelsExistingCreatedOrder() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        CancelOrderService service = new CancelOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(order);

        OrderResult result = service.cancelOrder(new CancelOrderCommand(order.getId()));

        assertEquals(order.getId().value(), result.id());
        assertEquals(order.getSellerId().value(), result.sellerId());
        assertEquals(OrderStatus.CANCELLED, result.status());
    }

    @Test
    void persistsUpdatedOrderAfterCancellation() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        CancelOrderService service = new CancelOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(order);

        service.cancelOrder(new CancelOrderCommand(order.getId()));

        assertEquals(1, orderRepository.saveCount());
        assertEquals(OrderStatus.CANCELLED, orderRepository.storedOrder(order.getId()).getStatus());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        CancelOrderService service = new CancelOrderService(orderRepository);
        OrderId missingOrderId = OrderId.random();

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> service.cancelOrder(new CancelOrderCommand(missingOrderId))
        );

        assertEquals(missingOrderId, exception.getOrderId());
    }

    @Test
    void propagatesInvalidTransitionWhenOrderCannotBeCancelled() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        CancelOrderService service = new CancelOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(new AssignedFulfillmentNodeId(UUID.randomUUID()), OffsetDateTime.parse("2026-06-28T10:15:30Z"));
        order.markReadyToShip();
        order.dispatch();
        order.deliver();
        orderRepository.store(order);

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> service.cancelOrder(new CancelOrderCommand(order.getId()))
        );

        assertEquals(OrderStatus.DELIVERED, exception.getCurrentStatus());
        assertEquals(OrderStatus.CANCELLED, exception.getTargetStatus());
        assertEquals(0, orderRepository.saveCount());
    }
}
