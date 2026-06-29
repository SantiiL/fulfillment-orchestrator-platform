package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeNotFoundException;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeRepository;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class AllocateOrderService implements AllocateOrderUseCase {

    private final OrderRepository orderRepository;
    private final FulfillmentNodeRepository fulfillmentNodeRepository;

    public AllocateOrderService(
            OrderRepository orderRepository,
            FulfillmentNodeRepository fulfillmentNodeRepository
    ) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.fulfillmentNodeRepository = Objects.requireNonNull(
                fulfillmentNodeRepository,
                "fulfillmentNodeRepository must not be null"
        );
    }

    @Override
    public OrderResult allocateOrder(AllocateOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        FulfillmentNodeId fulfillmentNodeId = new FulfillmentNodeId(command.fulfillmentNodeId().value());
        var fulfillmentNode = fulfillmentNodeRepository.findById(fulfillmentNodeId)
                .orElseThrow(() -> new FulfillmentNodeNotFoundException(fulfillmentNodeId));

        if (!fulfillmentNode.isActive()) {
            throw new FulfillmentNodeInactiveException(command.fulfillmentNodeId());
        }

        order.allocate(command.fulfillmentNodeId());
        orderRepository.save(order);

        return OrderResult.from(order);
    }
}
