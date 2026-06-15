package com.santilugani.fulfillmentorchestrator.orders.api;

import com.santilugani.fulfillmentorchestrator.orders.application.CreateOrderCommand;
import com.santilugani.fulfillmentorchestrator.orders.application.CreateOrderResult;
import com.santilugani.fulfillmentorchestrator.orders.application.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = Objects.requireNonNull(createOrderUseCase, "createOrderUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResult result = createOrderUseCase.createOrder(new CreateOrderCommand(request.sellerIdAsUuid()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateOrderResponse(result.id(), result.sellerId(), result.status()));
    }
}
