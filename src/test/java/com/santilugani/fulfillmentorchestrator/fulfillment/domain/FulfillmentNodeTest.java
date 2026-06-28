package com.santilugani.fulfillmentorchestrator.fulfillment.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FulfillmentNodeTest {

    @Test
    void createsActiveFulfillmentNodeByDefault() {
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
    }

    @Test
    void reconstitutesInactiveFulfillmentNode() {
        FulfillmentNode fulfillmentNode = FulfillmentNode.reconstitute(
                FulfillmentNodeId.random(),
                "AR-BUE-02",
                "Buenos Aires Node 2",
                50,
                false
        );

        assertFalse(fulfillmentNode.isActive());
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
}
