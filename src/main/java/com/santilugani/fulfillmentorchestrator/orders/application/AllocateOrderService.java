package com.santilugani.fulfillmentorchestrator.orders.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeNotFoundException;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeRepository;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
@Transactional
public class AllocateOrderService implements AllocateOrderUseCase {

    private final OrderRepository orderRepository;
    private final FulfillmentNodeRepository fulfillmentNodeRepository;
    private final Clock clock;

    @Autowired
    public AllocateOrderService(
            OrderRepository orderRepository,
            FulfillmentNodeRepository fulfillmentNodeRepository
    ) {
        this(orderRepository, fulfillmentNodeRepository, Clock.systemUTC());
    }

    AllocateOrderService(
            OrderRepository orderRepository,
            FulfillmentNodeRepository fulfillmentNodeRepository,
            Clock clock
    ) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.fulfillmentNodeRepository = Objects.requireNonNull(
                fulfillmentNodeRepository,
                "fulfillmentNodeRepository must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
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

        OffsetDateTime allocationTime = clock.instant().atOffset(ZoneOffset.UTC);
        OffsetDateTime dayStart = allocationTime.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime nextDayStart = dayStart.plusDays(1);

        order.validateCanAllocate();

        long currentAllocations = orderRepository.countAllocationsForFulfillmentNode(
                command.fulfillmentNodeId(),
                dayStart,
                nextDayStart
        );

        if (currentAllocations >= fulfillmentNode.getMaxDailyCapacity()) {
            throw new FulfillmentNodeCapacityExceededException(command.fulfillmentNodeId());
        }

        order.allocate(command.fulfillmentNodeId(), allocationTime);
        orderRepository.save(order);

        return OrderResult.from(order);
    }
}
