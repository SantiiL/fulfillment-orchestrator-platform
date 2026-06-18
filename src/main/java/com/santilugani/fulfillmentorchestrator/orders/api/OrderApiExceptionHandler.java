package com.santilugani.fulfillmentorchestrator.orders.api;

import com.santilugani.fulfillmentorchestrator.orders.application.OrderNotFoundException;
import com.santilugani.fulfillmentorchestrator.orders.domain.InvalidOrderStatusTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestControllerAdvice(basePackageClasses = OrderController.class)
public class OrderApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(
            OrderNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidOrderIdException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderId(
            InvalidOrderIdException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_ORDER_ID",
                "Order id must be a valid UUID",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderStatusTransition(
            InvalidOrderStatusTransitionException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "INVALID_ORDER_STATUS_TRANSITION",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), code, message, path, OffsetDateTime.now(ZoneOffset.UTC)));
    }
}
