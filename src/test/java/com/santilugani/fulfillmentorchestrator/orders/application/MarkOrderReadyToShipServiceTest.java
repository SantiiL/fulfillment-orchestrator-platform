package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.InvalidOrderStatusTransitionException;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import com.santilugani.fulfillmentorchestrator.orders.domain.SellerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarkOrderReadyToShipServiceTest {

    @Test
    void marksExistingAllocatedOrderAsReadyToShip() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        MarkOrderReadyToShipService service = new MarkOrderReadyToShipService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(new AssignedFulfillmentNodeId(UUID.randomUUID()));
        orderRepository.store(order);

        OrderResult result = service.markOrderReadyToShip(new MarkOrderReadyToShipCommand(order.getId()));

        assertEquals(order.getId().value(), result.id());
        assertEquals(order.getSellerId().value(), result.sellerId());
        assertEquals(OrderStatus.READY_TO_SHIP, result.status());
    }

    @Test
    void persistsUpdatedOrderAfterMarkingReadyToShip() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        MarkOrderReadyToShipService service = new MarkOrderReadyToShipService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate(new AssignedFulfillmentNodeId(UUID.randomUUID()));
        orderRepository.store(order);

        service.markOrderReadyToShip(new MarkOrderReadyToShipCommand(order.getId()));

        assertEquals(1, orderRepository.saveCount());
        assertEquals(OrderStatus.READY_TO_SHIP, orderRepository.storedOrder(order.getId()).getStatus());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        MarkOrderReadyToShipService service = new MarkOrderReadyToShipService(orderRepository);
        OrderId missingOrderId = OrderId.random();

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> service.markOrderReadyToShip(new MarkOrderReadyToShipCommand(missingOrderId))
        );

        assertEquals(missingOrderId, exception.getOrderId());
    }

    @Test
    void propagatesInvalidTransitionWhenOrderCannotBeMarkedReadyToShip() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        MarkOrderReadyToShipService service = new MarkOrderReadyToShipService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(order);

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> service.markOrderReadyToShip(new MarkOrderReadyToShipCommand(order.getId()))
        );

        assertEquals(OrderStatus.CREATED, exception.getCurrentStatus());
        assertEquals(OrderStatus.READY_TO_SHIP, exception.getTargetStatus());
        assertEquals(0, orderRepository.saveCount());
    }
}
