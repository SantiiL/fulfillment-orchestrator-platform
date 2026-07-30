package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplaceFulfillmentNodeWorkingDaysServiceTest {

    @Test
    void replacesWorkingDaysAndPersistsThem() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        ReplaceFulfillmentNodeWorkingDaysService service = new ReplaceFulfillmentNodeWorkingDaysService(
                fulfillmentNodeRepository
        );
        FulfillmentNode fulfillmentNode = new FulfillmentNode(
                FulfillmentNodeId.random(),
                "AR-BUE-01",
                "Buenos Aires Node 1",
                100
        );
        fulfillmentNodeRepository.store(fulfillmentNode);

        FulfillmentNodeWorkingDaysResult result = service.replaceWorkingDays(
                new ReplaceFulfillmentNodeWorkingDaysCommand(
                        fulfillmentNode.getId(),
                        Set.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
                )
        );

        assertEquals(1, fulfillmentNodeRepository.saveCount());
        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), result.workingDays());
        assertEquals(
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
                fulfillmentNodeRepository.storedFulfillmentNode(fulfillmentNode.getId()).getWorkingDays()
        );
    }

    @Test
    void replacesWorkingDaysIdempotently() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        ReplaceFulfillmentNodeWorkingDaysService service = new ReplaceFulfillmentNodeWorkingDaysService(
                fulfillmentNodeRepository
        );
        FulfillmentNode fulfillmentNode = new FulfillmentNode(
                FulfillmentNodeId.random(),
                "AR-BUE-02",
                "Buenos Aires Node 2",
                120
        );
        fulfillmentNode.replaceWorkingDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        fulfillmentNodeRepository.store(fulfillmentNode);
        ReplaceFulfillmentNodeWorkingDaysCommand command = new ReplaceFulfillmentNodeWorkingDaysCommand(
                fulfillmentNode.getId(),
                EnumSet.allOf(DayOfWeek.class)
        );

        FulfillmentNodeWorkingDaysResult firstResult = service.replaceWorkingDays(command);
        Set<DayOfWeek> firstPersistedWorkingDays = EnumSet.copyOf(
                fulfillmentNodeRepository.storedFulfillmentNode(fulfillmentNode.getId()).getWorkingDays()
        );

        FulfillmentNodeWorkingDaysResult secondResult = service.replaceWorkingDays(command);

        assertEquals(2, fulfillmentNodeRepository.saveCount());
        assertEquals(firstResult, secondResult);
        assertEquals(
                List.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                ),
                secondResult.workingDays()
        );
        assertEquals(EnumSet.allOf(DayOfWeek.class), firstPersistedWorkingDays);
        assertEquals(
                firstPersistedWorkingDays,
                fulfillmentNodeRepository.storedFulfillmentNode(fulfillmentNode.getId()).getWorkingDays()
        );
    }

    @Test
    void throwsWhenFulfillmentNodeDoesNotExist() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        ReplaceFulfillmentNodeWorkingDaysService service = new ReplaceFulfillmentNodeWorkingDaysService(
                fulfillmentNodeRepository
        );
        FulfillmentNodeId missingFulfillmentNodeId = FulfillmentNodeId.random();

        FulfillmentNodeNotFoundException exception = assertThrows(
                FulfillmentNodeNotFoundException.class,
                () -> service.replaceWorkingDays(
                        new ReplaceFulfillmentNodeWorkingDaysCommand(
                                missingFulfillmentNodeId,
                                Set.of(DayOfWeek.MONDAY)
                        )
                )
        );

        assertEquals(missingFulfillmentNodeId, exception.getFulfillmentNodeId());
        assertEquals("Fulfillment node was not found", exception.getMessage());
    }
}
