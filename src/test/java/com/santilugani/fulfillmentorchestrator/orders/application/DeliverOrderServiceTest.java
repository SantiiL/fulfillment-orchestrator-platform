package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.InvalidOrderStatusTransitionException;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import com.santilugani.fulfillmentorchestrator.orders.domain.SellerId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeliverOrderServiceTest {

    @Test
    void deliversExistingDispatchedOrder() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        DeliverOrderService service = new DeliverOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate();
        order.markReadyToShip();
        order.dispatch();
        orderRepository.store(order);

        OrderResult result = service.deliverOrder(new DeliverOrderCommand(order.getId()));

        assertEquals(order.getId().value(), result.id());
        assertEquals(order.getSellerId().value(), result.sellerId());
        assertEquals(OrderStatus.DELIVERED, result.status());
    }

    @Test
    void persistsUpdatedOrderAfterDelivery() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        DeliverOrderService service = new DeliverOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate();
        order.markReadyToShip();
        order.dispatch();
        orderRepository.store(order);

        service.deliverOrder(new DeliverOrderCommand(order.getId()));

        assertEquals(1, orderRepository.saveCount());
        assertEquals(OrderStatus.DELIVERED, orderRepository.storedOrder(order.getId()).getStatus());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        DeliverOrderService service = new DeliverOrderService(orderRepository);
        OrderId missingOrderId = OrderId.random();

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> service.deliverOrder(new DeliverOrderCommand(missingOrderId))
        );

        assertEquals(missingOrderId, exception.getOrderId());
    }

    @Test
    void propagatesInvalidTransitionWhenOrderCannotBeDelivered() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        DeliverOrderService service = new DeliverOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(order);

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> service.deliverOrder(new DeliverOrderCommand(order.getId()))
        );

        assertEquals(OrderStatus.CREATED, exception.getCurrentStatus());
        assertEquals(OrderStatus.DELIVERED, exception.getTargetStatus());
        assertEquals(0, orderRepository.saveCount());
    }
}
