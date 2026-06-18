package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.InvalidOrderStatusTransitionException;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import com.santilugani.fulfillmentorchestrator.orders.domain.SellerId;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelOrderServiceTest {

    @Test
    void cancelsExistingCreatedOrder() {
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        CancelOrderService service = new CancelOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.ordersById.put(order.getId(), order);

        CancelOrderResult result = service.cancelOrder(new CancelOrderCommand(order.getId()));

        assertEquals(order.getId().value(), result.id());
        assertEquals(order.getSellerId().value(), result.sellerId());
        assertEquals(OrderStatus.CANCELLED, result.status());
    }

    @Test
    void persistsUpdatedOrderAfterCancellation() {
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        CancelOrderService service = new CancelOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.ordersById.put(order.getId(), order);

        service.cancelOrder(new CancelOrderCommand(order.getId()));

        assertEquals(1, orderRepository.saveCount);
        assertEquals(OrderStatus.CANCELLED, orderRepository.ordersById.get(order.getId()).getStatus());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        FakeOrderRepository orderRepository = new FakeOrderRepository();
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
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        CancelOrderService service = new CancelOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate();
        order.markReadyToShip();
        order.dispatch();
        order.deliver();
        orderRepository.ordersById.put(order.getId(), order);

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> service.cancelOrder(new CancelOrderCommand(order.getId()))
        );

        assertEquals(OrderStatus.DELIVERED, exception.getCurrentStatus());
        assertEquals(OrderStatus.CANCELLED, exception.getTargetStatus());
        assertEquals(0, orderRepository.saveCount);
    }

    private static final class FakeOrderRepository implements OrderRepository {

        private final Map<OrderId, Order> ordersById = new HashMap<>();
        private int saveCount;

        @Override
        public void save(Order order) {
            saveCount++;
            ordersById.put(order.getId(), order);
        }

        @Override
        public java.util.Optional<Order> findById(OrderId orderId) {
            return java.util.Optional.ofNullable(ordersById.get(orderId));
        }
    }
}
