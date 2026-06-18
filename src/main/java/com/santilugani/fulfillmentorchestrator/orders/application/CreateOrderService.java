package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import com.santilugani.fulfillmentorchestrator.orders.domain.SellerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;

    public CreateOrderService(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
    }

    @Override
    public OrderResult createOrder(CreateOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Order order = new Order(OrderId.random(), new SellerId(command.sellerId()));
        orderRepository.save(order);

        return new OrderResult(
                order.getId().value(),
                order.getSellerId().value(),
                order.getStatus()
        );
    }
}
