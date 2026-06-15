package com.santilugani.fulfillmentorchestrator.orders.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OrderLifecyclePolicyTest {

    private final OrderLifecyclePolicy policy = new OrderLifecyclePolicy();

    @ParameterizedTest
    @MethodSource("validTransitions")
    void allowsConfiguredTransitions(OrderStatus currentStatus, OrderStatus targetStatus) {
        assertDoesNotThrow(() -> policy.validateTransition(currentStatus, targetStatus));
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void rejectsTransitionsOutsideTheLifecycle(OrderStatus currentStatus, OrderStatus targetStatus) {
        assertFalse(policy.canTransition(currentStatus, targetStatus));
        assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> policy.validateTransition(currentStatus, targetStatus)
        );
    }

    @Test
    void terminalStatusesHaveNoOutgoingTransitions() {
        for (OrderStatus terminalStatus : Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED)) {
            for (OrderStatus targetStatus : OrderStatus.values()) {
                assertFalse(policy.canTransition(terminalStatus, targetStatus));
            }
        }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validTransitions() {
        return Stream.of(
                arguments(OrderStatus.CREATED, OrderStatus.ALLOCATED),
                arguments(OrderStatus.CREATED, OrderStatus.CANCELLED),
                arguments(OrderStatus.ALLOCATED, OrderStatus.READY_TO_SHIP),
                arguments(OrderStatus.ALLOCATED, OrderStatus.CANCELLED),
                arguments(OrderStatus.READY_TO_SHIP, OrderStatus.DISPATCHED),
                arguments(OrderStatus.READY_TO_SHIP, OrderStatus.CANCELLED),
                arguments(OrderStatus.DISPATCHED, OrderStatus.DELIVERED)
        );
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidTransitions() {
        return Stream.of(OrderStatus.values())
                .flatMap(currentStatus -> Stream.of(OrderStatus.values())
                        .filter(targetStatus -> !isValidTransition(currentStatus, targetStatus))
                        .map(targetStatus -> arguments(currentStatus, targetStatus)));
    }

    private static boolean isValidTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        return switch (currentStatus) {
            case CREATED -> targetStatus == OrderStatus.ALLOCATED || targetStatus == OrderStatus.CANCELLED;
            case ALLOCATED -> targetStatus == OrderStatus.READY_TO_SHIP || targetStatus == OrderStatus.CANCELLED;
            case READY_TO_SHIP -> targetStatus == OrderStatus.DISPATCHED || targetStatus == OrderStatus.CANCELLED;
            case DISPATCHED -> targetStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
