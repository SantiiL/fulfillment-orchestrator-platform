package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CreateOrderServiceTest {

    @Test
    void createsOrderWithGeneratedIdAndCreatedStatus() {
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        CreateOrderService service = new CreateOrderService(orderRepository);
        UUID sellerId = UUID.randomUUID();

        CreateOrderResult result = service.createOrder(new CreateOrderCommand(sellerId));

        assertNotNull(result.id());
        assertEquals(sellerId, result.sellerId());
        assertEquals(OrderStatus.CREATED, result.status());
    }

    @Test
    void persistsCreatedOrderThroughRepository() {
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        CreateOrderService service = new CreateOrderService(orderRepository);
        UUID sellerId = UUID.randomUUID();

        CreateOrderResult result = service.createOrder(new CreateOrderCommand(sellerId));

        assertEquals(1, orderRepository.savedOrders.size());

        Order savedOrder = orderRepository.savedOrders.getFirst();
        assertEquals(result.id(), savedOrder.getId().value());
        assertEquals(sellerId, savedOrder.getSellerId().value());
        assertEquals(OrderStatus.CREATED, savedOrder.getStatus());
    }

    private static final class FakeOrderRepository implements OrderRepository {

        private final List<Order> savedOrders = new ArrayList<>();

        @Override
        public void save(Order order) {
            savedOrders.add(order);
        }

        @Override
        public Optional<Order> findById(com.santilugani.fulfillmentorchestrator.orders.domain.OrderId orderId) {
            return savedOrders.stream()
                    .filter(order -> order.getId().equals(orderId))
                    .findFirst();
        }
    }
}
