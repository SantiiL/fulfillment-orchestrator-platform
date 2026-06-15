package com.santilugani.fulfillmentorchestrator.orders.api;

import java.time.OffsetDateTime;

public record ApiErrorResponse(String code, String message, String path, OffsetDateTime timestamp) {
}
