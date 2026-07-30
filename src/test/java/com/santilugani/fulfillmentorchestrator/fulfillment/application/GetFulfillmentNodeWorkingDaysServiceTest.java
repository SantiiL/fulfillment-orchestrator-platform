package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetFulfillmentNodeWorkingDaysServiceTest {

    @Test
    void returnsFulfillmentNodeWorkingDaysInMondayToSundayOrder() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        GetFulfillmentNodeWorkingDaysService service = new GetFulfillmentNodeWorkingDaysService(
                fulfillmentNodeRepository
        );
        FulfillmentNode fulfillmentNode = FulfillmentNode.reconstitute(
                FulfillmentNodeId.random(),
                "AR-BUE-01",
                "Buenos Aires Node 1",
                100,
                true,
                EnumSet.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        );
        fulfillmentNodeRepository.store(fulfillmentNode);

        FulfillmentNodeWorkingDaysResult result = service.getWorkingDays(
                new GetFulfillmentNodeWorkingDaysQuery(fulfillmentNode.getId())
        );

        assertEquals(fulfillmentNode.getId().value(), result.id());
        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), result.workingDays());
    }

    @Test
    void throwsWhenFulfillmentNodeDoesNotExist() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        GetFulfillmentNodeWorkingDaysService service = new GetFulfillmentNodeWorkingDaysService(
                fulfillmentNodeRepository
        );
        FulfillmentNodeId missingFulfillmentNodeId = FulfillmentNodeId.random();

        FulfillmentNodeNotFoundException exception = assertThrows(
                FulfillmentNodeNotFoundException.class,
                () -> service.getWorkingDays(new GetFulfillmentNodeWorkingDaysQuery(missingFulfillmentNodeId))
        );

        assertEquals(missingFulfillmentNodeId, exception.getFulfillmentNodeId());
        assertEquals("Fulfillment node was not found", exception.getMessage());
    }
}
