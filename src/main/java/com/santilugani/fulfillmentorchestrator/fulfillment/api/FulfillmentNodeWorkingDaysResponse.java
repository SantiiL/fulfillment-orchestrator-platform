package com.santilugani.fulfillmentorchestrator.fulfillment.api;

import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeWorkingDaysResult;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record FulfillmentNodeWorkingDaysResponse(UUID id, List<DayOfWeek> workingDays) {

    public FulfillmentNodeWorkingDaysResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(workingDays, "workingDays must not be null");
        workingDays = List.copyOf(workingDays);
    }

    public static FulfillmentNodeWorkingDaysResponse from(FulfillmentNodeWorkingDaysResult result) {
        Objects.requireNonNull(result, "result must not be null");

        return new FulfillmentNodeWorkingDaysResponse(result.id(), result.workingDays());
    }
}
