package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record FulfillmentNodeWorkingDaysResult(UUID id, List<DayOfWeek> workingDays) {

    public FulfillmentNodeWorkingDaysResult {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(workingDays, "workingDays must not be null");
        workingDays = List.copyOf(workingDays);
    }

    public static FulfillmentNodeWorkingDaysResult from(FulfillmentNode fulfillmentNode) {
        Objects.requireNonNull(fulfillmentNode, "fulfillmentNode must not be null");

        return new FulfillmentNodeWorkingDaysResult(
                fulfillmentNode.getId().value(),
                fulfillmentNode.getWorkingDays().stream()
                        .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                        .toList()
        );
    }
}
