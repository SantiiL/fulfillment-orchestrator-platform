package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeNotFoundException;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeRepository;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.InvalidOrderStatusTransitionException;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import com.santilugani.fulfillmentorchestrator.orders.domain.SellerId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AllocateOrderServiceTest {

    private static final Instant ALLOCATION_INSTANT = Instant.parse("2026-06-28T10:15:30Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(ALLOCATION_INSTANT, ZoneOffset.UTC);

    @Test
    void allowsAllocationWhenNodeCapacityIsAvailable() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        TestAllocationFulfillmentNodeRepository fulfillmentNodeRepository = new TestAllocationFulfillmentNodeRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository, fulfillmentNodeRepository, FIXED_CLOCK);
        Order order = new Order(OrderId.random(), SellerId.random());
        FulfillmentNodeId fulfillmentNodeId = FulfillmentNodeId.random();
        fulfillmentNodeRepository.store(new FulfillmentNode(fulfillmentNodeId, "AR-BUE-01", "Buenos Aires Node 1", 100));
        orderRepository.store(order);

        OrderResult result = service.allocateOrder(
                new AllocateOrderCommand(order.getId(), new AssignedFulfillmentNodeId(fulfillmentNodeId.value()))
        );

        assertEquals(order.getId().value(), result.id());
        assertEquals(order.getSellerId().value(), result.sellerId());
        assertEquals(OrderStatus.ALLOCATED, result.status());
        assertEquals(fulfillmentNodeId.value(), result.assignedFulfillmentNodeId());
        assertEquals(ALLOCATION_INSTANT.atOffset(ZoneOffset.UTC), orderRepository.storedOrder(order.getId()).getAllocatedAt());
    }

    @Test
    void persistsUpdatedOrderAfterAllocation() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        TestAllocationFulfillmentNodeRepository fulfillmentNodeRepository = new TestAllocationFulfillmentNodeRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository, fulfillmentNodeRepository, FIXED_CLOCK);
        Order order = new Order(OrderId.random(), SellerId.random());
        FulfillmentNodeId fulfillmentNodeId = FulfillmentNodeId.random();
        fulfillmentNodeRepository.store(new FulfillmentNode(fulfillmentNodeId, "AR-BUE-01", "Buenos Aires Node 1", 100));
        orderRepository.store(order);

        service.allocateOrder(new AllocateOrderCommand(order.getId(), new AssignedFulfillmentNodeId(fulfillmentNodeId.value())));

        assertEquals(1, orderRepository.saveCount());
        assertEquals(OrderStatus.ALLOCATED, orderRepository.storedOrder(order.getId()).getStatus());
        assertEquals(
                fulfillmentNodeId.value(),
                orderRepository.storedOrder(order.getId()).getAssignedFulfillmentNodeId().value()
        );
        assertEquals(ALLOCATION_INSTANT.atOffset(ZoneOffset.UTC), orderRepository.storedOrder(order.getId()).getAllocatedAt());
    }

    @Test
    void rejectsAllocationWhenNodeCapacityIsReached() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        TestAllocationFulfillmentNodeRepository fulfillmentNodeRepository = new TestAllocationFulfillmentNodeRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository, fulfillmentNodeRepository, FIXED_CLOCK);
        FulfillmentNodeId fulfillmentNodeId = FulfillmentNodeId.random();
        fulfillmentNodeRepository.store(new FulfillmentNode(fulfillmentNodeId, "AR-BUE-01", "Buenos Aires Node 1", 1));

        Order existingAllocatedOrder = Order.reconstitute(
                OrderId.random(),
                SellerId.random(),
                OrderStatus.ALLOCATED,
                new AssignedFulfillmentNodeId(fulfillmentNodeId.value()),
                ALLOCATION_INSTANT.minusSeconds(60).atOffset(ZoneOffset.UTC)
        );
        Order orderToAllocate = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(existingAllocatedOrder);
        orderRepository.store(orderToAllocate);

        FulfillmentNodeCapacityExceededException exception = assertThrows(
                FulfillmentNodeCapacityExceededException.class,
                () -> service.allocateOrder(
                        new AllocateOrderCommand(orderToAllocate.getId(), new AssignedFulfillmentNodeId(fulfillmentNodeId.value()))
                )
        );

        assertEquals(fulfillmentNodeId.value(), exception.getFulfillmentNodeId().value());
        assertEquals(OrderStatus.CREATED, orderRepository.storedOrder(orderToAllocate.getId()).getStatus());
        assertEquals(0, orderRepository.saveCount());
    }

    @Test
    void ignoresLegacyAllocatedOrdersWithoutAllocationTimestamp() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        TestAllocationFulfillmentNodeRepository fulfillmentNodeRepository = new TestAllocationFulfillmentNodeRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository, fulfillmentNodeRepository, FIXED_CLOCK);
        FulfillmentNodeId fulfillmentNodeId = FulfillmentNodeId.random();
        fulfillmentNodeRepository.store(new FulfillmentNode(fulfillmentNodeId, "AR-BUE-01", "Buenos Aires Node 1", 1));

        orderRepository.store(
                Order.reconstitute(
                        OrderId.random(),
                        SellerId.random(),
                        OrderStatus.ALLOCATED,
                        new AssignedFulfillmentNodeId(fulfillmentNodeId.value())
                )
        );

        Order orderToAllocate = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(orderToAllocate);

        OrderResult result = service.allocateOrder(
                new AllocateOrderCommand(orderToAllocate.getId(), new AssignedFulfillmentNodeId(fulfillmentNodeId.value()))
        );

        assertEquals(OrderStatus.ALLOCATED, result.status());
        assertEquals(1, orderRepository.saveCount());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        TestAllocationFulfillmentNodeRepository fulfillmentNodeRepository = new TestAllocationFulfillmentNodeRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository, fulfillmentNodeRepository, FIXED_CLOCK);
        OrderId missingOrderId = OrderId.random();
        AssignedFulfillmentNodeId fulfillmentNodeId = new AssignedFulfillmentNodeId(UUID.randomUUID());

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> service.allocateOrder(new AllocateOrderCommand(missingOrderId, fulfillmentNodeId))
        );

        assertEquals(missingOrderId, exception.getOrderId());
    }

    @Test
    void throwsWhenFulfillmentNodeDoesNotExist() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        Order order = new Order(OrderId.random(), SellerId.random());
        orderRepository.store(order);
        TestAllocationFulfillmentNodeRepository fulfillmentNodeRepository = new TestAllocationFulfillmentNodeRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository, fulfillmentNodeRepository, FIXED_CLOCK);
        AssignedFulfillmentNodeId fulfillmentNodeId = new AssignedFulfillmentNodeId(UUID.randomUUID());

        FulfillmentNodeNotFoundException exception = assertThrows(
                FulfillmentNodeNotFoundException.class,
                () -> service.allocateOrder(new AllocateOrderCommand(order.getId(), fulfillmentNodeId))
        );

        assertEquals(fulfillmentNodeId.value(), exception.getFulfillmentNodeId().value());
        assertEquals(0, orderRepository.saveCount());
    }

    @Test
    void throwsWhenFulfillmentNodeIsInactive() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        TestAllocationFulfillmentNodeRepository fulfillmentNodeRepository = new TestAllocationFulfillmentNodeRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository, fulfillmentNodeRepository, FIXED_CLOCK);
        Order order = new Order(OrderId.random(), SellerId.random());
        FulfillmentNodeId fulfillmentNodeId = FulfillmentNodeId.random();
        fulfillmentNodeRepository.store(
                FulfillmentNode.reconstitute(fulfillmentNodeId, "AR-BUE-01", "Buenos Aires Node 1", 100, false)
        );
        orderRepository.store(order);

        FulfillmentNodeInactiveException exception = assertThrows(
                FulfillmentNodeInactiveException.class,
                () -> service.allocateOrder(
                        new AllocateOrderCommand(order.getId(), new AssignedFulfillmentNodeId(fulfillmentNodeId.value()))
                )
        );

        assertEquals(fulfillmentNodeId.value(), exception.getFulfillmentNodeId().value());
        assertEquals(0, orderRepository.saveCount());
    }

    @Test
    void propagatesInvalidTransitionWhenOrderCannotBeAllocated() {
        TestOrderRepository orderRepository = new TestOrderRepository();
        TestAllocationFulfillmentNodeRepository fulfillmentNodeRepository = new TestAllocationFulfillmentNodeRepository();
        AllocateOrderService service = new AllocateOrderService(orderRepository, fulfillmentNodeRepository, FIXED_CLOCK);
        Order order = new Order(OrderId.random(), SellerId.random());
        FulfillmentNodeId fulfillmentNodeId = FulfillmentNodeId.random();
        fulfillmentNodeRepository.store(new FulfillmentNode(fulfillmentNodeId, "AR-BUE-01", "Buenos Aires Node 1", 100));
        order.cancel();
        orderRepository.store(order);

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> service.allocateOrder(
                        new AllocateOrderCommand(order.getId(), new AssignedFulfillmentNodeId(fulfillmentNodeId.value()))
                )
        );

        assertEquals(OrderStatus.CANCELLED, exception.getCurrentStatus());
        assertEquals(OrderStatus.ALLOCATED, exception.getTargetStatus());
        assertEquals(0, orderRepository.saveCount());
    }

    private static final class TestAllocationFulfillmentNodeRepository implements FulfillmentNodeRepository {

        private final Map<FulfillmentNodeId, FulfillmentNode> fulfillmentNodesById = new HashMap<>();

        void store(FulfillmentNode fulfillmentNode) {
            fulfillmentNodesById.put(fulfillmentNode.getId(), fulfillmentNode);
        }

        @Override
        public void save(FulfillmentNode fulfillmentNode) {
            fulfillmentNodesById.put(fulfillmentNode.getId(), fulfillmentNode);
        }

        @Override
        public Optional<FulfillmentNode> findById(FulfillmentNodeId fulfillmentNodeId) {
            return Optional.ofNullable(fulfillmentNodesById.get(fulfillmentNodeId));
        }

        @Override
        public java.util.List<FulfillmentNode> findAll() {
            return java.util.List.copyOf(fulfillmentNodesById.values());
        }

        @Override
        public boolean existsByCode(String code) {
            return fulfillmentNodesById.values().stream()
                    .anyMatch(fulfillmentNode -> fulfillmentNode.getCode().equals(code));
        }
    }
}
