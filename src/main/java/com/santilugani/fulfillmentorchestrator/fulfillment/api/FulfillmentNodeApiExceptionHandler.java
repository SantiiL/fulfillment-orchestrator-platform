package com.santilugani.fulfillmentorchestrator.fulfillment.api;

import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeCodeAlreadyExistsException;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestControllerAdvice(basePackageClasses = FulfillmentNodeController.class)
public class FulfillmentNodeApiExceptionHandler {

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
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(FulfillmentNodeCodeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateCode(
            FulfillmentNodeCodeAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "FULFILLMENT_NODE_CODE_ALREADY_EXISTS",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        String message = fieldError == null ? "Fulfillment node request is invalid" : fieldError.getDefaultMessage();

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_FULFILLMENT_NODE_REQUEST",
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidFulfillmentNodeWorkingDaysRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidWorkingDaysRequest(
            InvalidFulfillmentNodeWorkingDaysRequestException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_FULFILLMENT_NODE_WORKING_DAYS_REQUEST",
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
