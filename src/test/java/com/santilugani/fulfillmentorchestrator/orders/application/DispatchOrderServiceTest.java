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

class DispatchOrderServiceTest {

    @Test
    void dispatchesExistingReadyToShipOrder() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        DispatchOrderService service = new DispatchOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(new AssignedFulfillmentNodeId(UUID.randomUUID()), OffsetDateTime.parse("2026-06-28T10:15:30Z"));
        order.markReadyToShip();
        orderRepository.store(order);

        OrderResult result = service.dispatchOrder(new DispatchOrderCommand(order.getId()));

        assertEquals(order.getId().value(), result.id());
        assertEquals(order.getSellerId().value(), result.sellerId());
        assertEquals(OrderStatus.DISPATCHED, result.status());
    }

    @Test
    void persistsUpdatedOrderAfterDispatch() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        DispatchOrderService service = new DispatchOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(new AssignedFulfillmentNodeId(UUID.randomUUID()), OffsetDateTime.parse("2026-06-28T10:15:30Z"));
        order.markReadyToShip();
        orderRepository.store(order);

        service.dispatchOrder(new DispatchOrderCommand(order.getId()));

        assertEquals(1, orderRepository.saveCount());
        assertEquals(OrderStatus.DISPATCHED, orderRepository.storedOrder(order.getId()).getStatus());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        DispatchOrderService service = new DispatchOrderService(orderRepository);
        OrderId missingOrderId = OrderId.random();

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> service.dispatchOrder(new DispatchOrderCommand(missingOrderId))
        );

        assertEquals(missingOrderId, exception.getOrderId());
    }

    @Test
    void propagatesInvalidTransitionWhenOrderCannotBeDispatched() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        DispatchOrderService service = new DispatchOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(order);

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> service.dispatchOrder(new DispatchOrderCommand(order.getId()))
        );

        assertEquals(OrderStatus.CREATED, exception.getCurrentStatus());
        assertEquals(OrderStatus.DISPATCHED, exception.getTargetStatus());
        assertEquals(0, orderRepository.saveCount());
    }
}
