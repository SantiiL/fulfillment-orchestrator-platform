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

class DeliverOrderServiceTest {

    @Test
    void deliversExistingDispatchedOrder() {
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        DeliverOrderService service = new DeliverOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate();
        order.markReadyToShip();
        order.dispatch();
        orderRepository.ordersById.put(order.getId(), order);

        OrderResult result = service.deliverOrder(new DeliverOrderCommand(order.getId()));

        assertEquals(order.getId().value(), result.id());
        assertEquals(order.getSellerId().value(), result.sellerId());
        assertEquals(OrderStatus.DELIVERED, result.status());
    }

    @Test
    void persistsUpdatedOrderAfterDelivery() {
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        DeliverOrderService service = new DeliverOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        order.allocate();
        order.markReadyToShip();
        order.dispatch();
        orderRepository.ordersById.put(order.getId(), order);

        service.deliverOrder(new DeliverOrderCommand(order.getId()));

        assertEquals(1, orderRepository.saveCount);
        assertEquals(OrderStatus.DELIVERED, orderRepository.ordersById.get(order.getId()).getStatus());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        FakeOrderRepository orderRepository = new FakeOrderRepository();
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
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        DeliverOrderService service = new DeliverOrderService(orderRepository);
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.ordersById.put(order.getId(), order);

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> service.deliverOrder(new DeliverOrderCommand(order.getId()))
        );

        assertEquals(OrderStatus.CREATED, exception.getCurrentStatus());
        assertEquals(OrderStatus.DELIVERED, exception.getTargetStatus());
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
