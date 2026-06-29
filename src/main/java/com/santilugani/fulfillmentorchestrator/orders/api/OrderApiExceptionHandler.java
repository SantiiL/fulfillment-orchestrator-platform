package com.santilugani.fulfillmentorchestrator.orders.api;

import com.santilugani.fulfillmentorchestrator.orders.application.FulfillmentNodeCapacityExceededException;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeNotFoundException;
import com.santilugani.fulfillmentorchestrator.orders.application.FulfillmentNodeInactiveException;
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

    @ExceptionHandler(FulfillmentNodeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleFulfillmentNodeNotFound(
            FulfillmentNodeNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "FULFILLMENT_NODE_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidFulfillmentNodeIdException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidFulfillmentNodeId(
            InvalidFulfillmentNodeIdException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_FULFILLMENT_NODE_ID",
                "Fulfillment node id must be a valid UUID",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MissingFulfillmentNodeIdException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingFulfillmentNodeId(
            MissingFulfillmentNodeIdException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_FULFILLMENT_NODE_ID",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(FulfillmentNodeInactiveException.class)
    public ResponseEntity<ApiErrorResponse> handleFulfillmentNodeInactive(
            FulfillmentNodeInactiveException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "FULFILLMENT_NODE_INACTIVE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(FulfillmentNodeCapacityExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleFulfillmentNodeCapacityExceeded(
            FulfillmentNodeCapacityExceededException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "FULFILLMENT_NODE_CAPACITY_EXCEEDED",
                exception.getMessage(),
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
