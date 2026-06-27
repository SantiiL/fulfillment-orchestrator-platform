package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.InvalidOrderStatusTransitionException;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import com.santilugani.fulfillmentorchestrator.orders.domain.SellerId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AllocateOrderServiceTest {

    @Test
    void allocatesExistingCreatedOrder() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(order);

        OrderResult result = service.allocateOrder(new AllocateOrderCommand(order.getId()));

        assertEquals(order.getId().value(), result.id());
        assertEquals(order.getSellerId().value(), result.sellerId());
        assertEquals(OrderStatus.ALLOCATED, result.status());
    }

    @Test
    void persistsUpdatedOrderAfterAllocation() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(order);

        service.allocateOrder(new AllocateOrderCommand(order.getId()));

        assertEquals(1, orderRepository.saveCount());
        assertEquals(OrderStatus.ALLOCATED, orderRepository.storedOrder(order.getId()).getStatus());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository);
        OrderId missingOrderId = OrderId.random();

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> service.allocateOrder(new AllocateOrderCommand(missingOrderId))
        );

        assertEquals(missingOrderId, exception.getOrderId());
    }

    @Test
    void propagatesInvalidTransitionWhenOrderCannotBeAllocated() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.cancel();
        orderRepository.store(order);

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> service.allocateOrder(new AllocateOrderCommand(order.getId()))
        );

        assertEquals(OrderStatus.CANCELLED, exception.getCurrentStatus());
        assertEquals(OrderStatus.ALLOCATED, exception.getTargetStatus());
        assertEquals(0, orderRepository.saveCount());
    }
}
