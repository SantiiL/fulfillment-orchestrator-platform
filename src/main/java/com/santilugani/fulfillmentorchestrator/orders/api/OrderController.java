package com.santilugani.fulfillmentorchestrator.orders.api;

import com.santilugani.fulfillmentorchestrator.orders.application.AllocateOrderCommand;
import com.santilugani.fulfillmentorchestrator.orders.application.AllocateOrderUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.CancelOrderCommand;
import com.santilugani.fulfillmentorchestrator.orders.application.CancelOrderUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.CreateOrderCommand;
import com.santilugani.fulfillmentorchestrator.orders.application.CreateOrderUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.DeliverOrderCommand;
import com.santilugani.fulfillmentorchestrator.orders.application.DeliverOrderUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.DispatchOrderCommand;
import com.santilugani.fulfillmentorchestrator.orders.application.DispatchOrderUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.GetOrderByIdQuery;
import com.santilugani.fulfillmentorchestrator.orders.application.GetOrderByIdUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.MarkOrderReadyToShipCommand;
import com.santilugani.fulfillmentorchestrator.orders.application.MarkOrderReadyToShipUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.OrderResult;
import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final AllocateOrderUseCase allocateOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final CreateOrderUseCase createOrderUseCase;
    private final DeliverOrderUseCase deliverOrderUseCase;
    private final DispatchOrderUseCase dispatchOrderUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final MarkOrderReadyToShipUseCase markOrderReadyToShipUseCase;

    public OrderController(
            AllocateOrderUseCase allocateOrderUseCase,
            CancelOrderUseCase cancelOrderUseCase,
            CreateOrderUseCase createOrderUseCase,
            DeliverOrderUseCase deliverOrderUseCase,
            DispatchOrderUseCase dispatchOrderUseCase,
            GetOrderByIdUseCase getOrderByIdUseCase,
            MarkOrderReadyToShipUseCase markOrderReadyToShipUseCase
    ) {
        this.allocateOrderUseCase = Objects.requireNonNull(
                allocateOrderUseCase,
                "allocateOrderUseCase must not be null"
        );
        this.cancelOrderUseCase = Objects.requireNonNull(cancelOrderUseCase, "cancelOrderUseCase must not be null");
        this.createOrderUseCase = Objects.requireNonNull(createOrderUseCase, "createOrderUseCase must not be null");
        this.deliverOrderUseCase = Objects.requireNonNull(deliverOrderUseCase, "deliverOrderUseCase must not be null");
        this.dispatchOrderUseCase = Objects.requireNonNull(dispatchOrderUseCase, "dispatchOrderUseCase must not be null");
        this.getOrderByIdUseCase = Objects.requireNonNull(getOrderByIdUseCase, "getOrderByIdUseCase must not be null");
        this.markOrderReadyToShipUseCase = Objects.requireNonNull(
                markOrderReadyToShipUseCase,
                "markOrderReadyToShipUseCase must not be null"
        );
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResult result = createOrderUseCase.createOrder(new CreateOrderCommand(request.sellerIdAsUuid()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderResponse(
                        result.id(),
                        result.sellerId(),
                        result.status(),
                        result.assignedFulfillmentNodeId()
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id) {
        OrderResult result = getOrderByIdUseCase.getOrderById(new GetOrderByIdQuery(parseOrderId(id)));

        return ResponseEntity.ok(toOrderResponse(result));
    }

    @PostMapping("/{id}/allocate")
    public ResponseEntity<OrderResponse> allocateOrder(
            @PathVariable String id,
            @RequestBody(required = false) AllocateOrderRequest request
    ) {
        OrderResult result = allocateOrderUseCase.allocateOrder(
                new AllocateOrderCommand(parseOrderId(id), parseFulfillmentNodeId(request))
        );

        return ResponseEntity.ok(toOrderResponse(result));
    }

    @PostMapping("/{id}/ready-to-ship")
    public ResponseEntity<OrderResponse> markOrderReadyToShip(@PathVariable String id) {
        OrderResult result = markOrderReadyToShipUseCase.markOrderReadyToShip(
                new MarkOrderReadyToShipCommand(parseOrderId(id))
        );

        return ResponseEntity.ok(toOrderResponse(result));
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<OrderResponse> dispatchOrder(@PathVariable String id) {
        OrderResult result = dispatchOrderUseCase.dispatchOrder(new DispatchOrderCommand(parseOrderId(id)));

        return ResponseEntity.ok(toOrderResponse(result));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<OrderResponse> deliverOrder(@PathVariable String id) {
        OrderResult result = deliverOrderUseCase.deliverOrder(new DeliverOrderCommand(parseOrderId(id)));

        return ResponseEntity.ok(toOrderResponse(result));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String id) {
        OrderResult result = cancelOrderUseCase.cancelOrder(new CancelOrderCommand(parseOrderId(id)));

        return ResponseEntity.ok(toOrderResponse(result));
    }

    private OrderId parseOrderId(String id) {
        try {
            return OrderId.from(id);
        } catch (IllegalArgumentException exception) {
            throw new InvalidOrderIdException(id);
        }
    }

    private AssignedFulfillmentNodeId parseFulfillmentNodeId(AllocateOrderRequest request) {
        if (request == null || request.fulfillmentNodeId() == null || request.fulfillmentNodeId().isBlank()) {
            throw new MissingFulfillmentNodeIdException();
        }

        try {
            return AssignedFulfillmentNodeId.from(request.fulfillmentNodeId());
        } catch (IllegalArgumentException exception) {
            throw new InvalidFulfillmentNodeIdException(request.fulfillmentNodeId());
        }
    }

    private OrderResponse toOrderResponse(OrderResult result) {
        return new OrderResponse(
                result.id(),
                result.sellerId(),
                result.status(),
                result.assignedFulfillmentNodeId()
        );
    }
}
