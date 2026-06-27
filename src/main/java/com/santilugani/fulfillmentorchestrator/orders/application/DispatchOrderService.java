package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class DispatchOrderService implements DispatchOrderUseCase {

    private final OrderRepository orderRepository;

    public DispatchOrderService(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
    }

    @Override
    public OrderResult dispatchOrder(DispatchOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        order.dispatch();
        orderRepository.save(order);

        return new OrderResult(
                order.getId().value(),
                order.getSellerId().value(),
                order.getStatus()
        );
    }
}
