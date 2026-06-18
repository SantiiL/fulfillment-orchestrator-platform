package com.santilugani.fulfillmentorchestrator.orders.api;

import com.santilugani.fulfillmentorchestrator.orders.application.CancelOrderCommand;
import com.santilugani.fulfillmentorchestrator.orders.application.CancelOrderUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.CreateOrderCommand;
import com.santilugani.fulfillmentorchestrator.orders.application.CreateOrderUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.GetOrderByIdQuery;
import com.santilugani.fulfillmentorchestrator.orders.application.GetOrderByIdUseCase;
import com.santilugani.fulfillmentorchestrator.orders.application.OrderResult;
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

    private final CancelOrderUseCase cancelOrderUseCase;
    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;

    public OrderController(
            CancelOrderUseCase cancelOrderUseCase,
            CreateOrderUseCase createOrderUseCase,
            GetOrderByIdUseCase getOrderByIdUseCase
    ) {
        this.cancelOrderUseCase = Objects.requireNonNull(cancelOrderUseCase, "cancelOrderUseCase must not be null");
        this.createOrderUseCase = Objects.requireNonNull(createOrderUseCase, "createOrderUseCase must not be null");
        this.getOrderByIdUseCase = Objects.requireNonNull(getOrderByIdUseCase, "getOrderByIdUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResult result = createOrderUseCase.createOrder(new CreateOrderCommand(request.sellerIdAsUuid()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateOrderResponse(result.id(), result.sellerId(), result.status()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetOrderResponse> getOrderById(@PathVariable String id) {
        OrderResult result = getOrderByIdUseCase.getOrderById(new GetOrderByIdQuery(parseOrderId(id)));

        return ResponseEntity.ok(new GetOrderResponse(result.id(), result.sellerId(), result.status()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<GetOrderResponse> cancelOrder(@PathVariable String id) {
        OrderResult result = cancelOrderUseCase.cancelOrder(new CancelOrderCommand(parseOrderId(id)));

        return ResponseEntity.ok(new GetOrderResponse(result.id(), result.sellerId(), result.status()));
    }

    private OrderId parseOrderId(String id) {
        try {
            return OrderId.from(id);
        } catch (IllegalArgumentException exception) {
            throw new InvalidOrderIdException(id);
        }
    }
}
