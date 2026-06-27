package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class MarkOrderReadyToShipService implements MarkOrderReadyToShipUseCase {

    private final OrderRepository orderRepository;

    public MarkOrderReadyToShipService(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
    }

    @Override
    public OrderResult markOrderReadyToShip(MarkOrderReadyToShipCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        order.markReadyToShip();
        orderRepository.save(order);

        return new OrderResult(
                order.getId().value(),
                order.getSellerId().value(),
                order.getStatus()
        );
    }
}
