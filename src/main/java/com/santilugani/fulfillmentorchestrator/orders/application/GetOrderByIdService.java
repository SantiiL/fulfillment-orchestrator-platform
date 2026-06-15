package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class GetOrderByIdService implements GetOrderByIdUseCase {

    private final OrderRepository orderRepository;

    public GetOrderByIdService(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
    }

    @Override
    public GetOrderByIdResult getOrderById(GetOrderByIdQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Order order = orderRepository.findById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        return new GetOrderByIdResult(
                order.getId().value(),
                order.getSellerId().value(),
                order.getStatus()
        );
    }
}
