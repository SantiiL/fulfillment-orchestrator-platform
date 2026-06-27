package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderRepository orderRepository;

    public CancelOrderService(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
    }

    @Override
    public OrderResult cancelOrder(CancelOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        order.cancel();
        orderRepository.save(order);

        return OrderResult.from(order);
    }
}
