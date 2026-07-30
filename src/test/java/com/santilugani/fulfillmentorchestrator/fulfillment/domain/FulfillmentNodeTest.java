package com.santilugani.fulfillmentorchestrator.fulfillment.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FulfillmentNodeTest {

    @Test
    void createsActiveFulfillmentNodeByDefaultWithAllWorkingDays() {
        FulfillmentNodeId fulfillmentNodeId = FulfillmentNodeId.random();

        FulfillmentNode fulfillmentNode = new FulfillmentNode(
                fulfillmentNodeId,
                " AR-BUE-01 ",
                " Buenos Aires Node 1 ",
                100
        );

        assertEquals(fulfillmentNodeId, fulfillmentNode.getId());
        assertEquals("AR-BUE-01", fulfillmentNode.getCode());
        assertEquals("Buenos Aires Node 1", fulfillmentNode.getName());
        assertEquals(100, fulfillmentNode.getMaxDailyCapacity());
        assertTrue(fulfillmentNode.isActive());
        assertEquals(EnumSet.allOf(DayOfWeek.class), fulfillmentNode.getWorkingDays());
    }

    @Test
    void reconstitutesInactiveFulfillmentNodeWithPersistedWorkingDays() {
        Set<DayOfWeek> persistedWorkingDays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

        FulfillmentNode fulfillmentNode = FulfillmentNode.reconstitute(
                FulfillmentNodeId.random(),
                "AR-BUE-02",
                "Buenos Aires Node 2",
                50,
                false,
                persistedWorkingDays
        );

        assertFalse(fulfillmentNode.isActive());
        assertEquals(persistedWorkingDays, fulfillmentNode.getWorkingDays());
    }

    @Test
    void rejectsNullId() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new FulfillmentNode(null, "AR-BUE-01", "Buenos Aires Node 1", 100)
        );

        assertEquals("id must not be null", exception.getMessage());
    }

    @Test
    void rejectsBlankCode() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FulfillmentNode(FulfillmentNodeId.random(), "   ", "Buenos Aires Node 1", 100)
        );

        assertEquals("code must not be blank", exception.getMessage());
    }

    @Test
    void rejectsBlankName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FulfillmentNode(FulfillmentNodeId.random(), "AR-BUE-01", "   ", 100)
        );

        assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    void rejectsNonPositiveMaxDailyCapacity() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FulfillmentNode(FulfillmentNodeId.random(), "AR-BUE-01", "Buenos Aires Node 1", 0)
        );

        assertEquals("maxDailyCapacity must be positive", exception.getMessage());
    }

    @Test
    void replacesWorkingDays() {
        FulfillmentNode fulfillmentNode = new FulfillmentNode(
                FulfillmentNodeId.random(),
                "AR-BUE-01",
                "Buenos Aires Node 1",
                100
        );

        fulfillmentNode.replaceWorkingDays(EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));

        assertEquals(EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), fulfillmentNode.getWorkingDays());
    }

    @Test
    void reportsWhetherDayIsConfiguredAsWorkingDay() {
        FulfillmentNode fulfillmentNode = FulfillmentNode.reconstitute(
                FulfillmentNodeId.random(),
                "AR-BUE-03",
                "Buenos Aires Node 3",
                150,
                true,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );

        assertTrue(fulfillmentNode.isWorkingDay(DayOfWeek.MONDAY));
        assertFalse(fulfillmentNode.isWorkingDay(DayOfWeek.SUNDAY));
    }

    @Test
    void defensivelyCopiesWorkingDaysOnReconstitutionAndPreventsExternalMutation() {
        Set<DayOfWeek> workingDays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        FulfillmentNode fulfillmentNode = FulfillmentNode.reconstitute(
                FulfillmentNodeId.random(),
                "AR-BUE-04",
                "Buenos Aires Node 4",
                120,
                true,
                workingDays
        );

        workingDays.add(DayOfWeek.FRIDAY);

        assertEquals(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), fulfillmentNode.getWorkingDays());
        assertThrows(UnsupportedOperationException.class, () -> fulfillmentNode.getWorkingDays().add(DayOfWeek.SUNDAY));
    }

    @Test
    void defensivelyCopiesWorkingDaysOnReplacement() {
        FulfillmentNode fulfillmentNode = new FulfillmentNode(
                FulfillmentNodeId.random(),
                "AR-BUE-05",
                "Buenos Aires Node 5",
                90
        );
        Set<DayOfWeek> replacementWorkingDays = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        fulfillmentNode.replaceWorkingDays(replacementWorkingDays);
        replacementWorkingDays.add(DayOfWeek.FRIDAY);

        assertEquals(EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), fulfillmentNode.getWorkingDays());
    }

    @Test
    void rejectsNullWorkingDaysOnReconstitution() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> FulfillmentNode.reconstitute(
                        FulfillmentNodeId.random(),
                        "AR-BUE-06",
                        "Buenos Aires Node 6",
                        100,
                        true,
                        null
                )
        );

        assertEquals("workingDays must not be null", exception.getMessage());
    }

    @Test
    void rejectsEmptyWorkingDaysOnReconstitution() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FulfillmentNode.reconstitute(
                        FulfillmentNodeId.random(),
                        "AR-BUE-07",
                        "Buenos Aires Node 7",
                        100,
                        true,
                        EnumSet.noneOf(DayOfWeek.class)
                )
        );

        assertEquals("workingDays must not be empty", exception.getMessage());
    }

    @Test
    void rejectsNullWorkingDaysOnReplacement() {
        FulfillmentNode fulfillmentNode = new FulfillmentNode(
                FulfillmentNodeId.random(),
                "AR-BUE-08",
                "Buenos Aires Node 8",
                100
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> fulfillmentNode.replaceWorkingDays(null)
        );

        assertEquals("workingDays must not be null", exception.getMessage());
    }

    @Test
    void rejectsEmptyWorkingDaysOnReplacement() {
        FulfillmentNode fulfillmentNode = new FulfillmentNode(
                FulfillmentNodeId.random(),
                "AR-BUE-09",
                "Buenos Aires Node 9",
                100
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fulfillmentNode.replaceWorkingDays(EnumSet.noneOf(DayOfWeek.class))
        );

        assertEquals("workingDays must not be empty", exception.getMessage());
    }
}
