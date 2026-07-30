package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record ReplaceFulfillmentNodeWorkingDaysCommand(
        FulfillmentNodeId fulfillmentNodeId,
        Set<DayOfWeek> workingDays
) {

    public ReplaceFulfillmentNodeWorkingDaysCommand {
        Objects.requireNonNull(fulfillmentNodeId, "fulfillmentNodeId must not be null");
        Objects.requireNonNull(workingDays, "workingDays must not be null");

        if (workingDays.isEmpty()) {
            throw new IllegalArgumentException("workingDays must not be empty");
        }

        for (DayOfWeek workingDay : workingDays) {
            if (workingDay == null) {
                throw new IllegalArgumentException("workingDays must not contain null");
            }
        }

        workingDays = Collections.unmodifiableSet(EnumSet.copyOf(workingDays));
    }
}
