package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetFulfillmentNodeByIdServiceTest {

    @Test
    void returnsFulfillmentNodeWhenItExists() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        GetFulfillmentNodeByIdService service = new GetFulfillmentNodeByIdService(fulfillmentNodeRepository);
        FulfillmentNode fulfillmentNode = new FulfillmentNode(
                FulfillmentNodeId.random(),
                "AR-BUE-01",
                "Buenos Aires Node 1",
                100
        );
        fulfillmentNodeRepository.store(fulfillmentNode);

        FulfillmentNodeResult result = service.getFulfillmentNodeById(
                new GetFulfillmentNodeByIdQuery(fulfillmentNode.getId())
        );

        assertEquals(fulfillmentNode.getId().value(), result.id());
        assertEquals("AR-BUE-01", result.code());
        assertEquals("Buenos Aires Node 1", result.name());
        assertEquals(100, result.maxDailyCapacity());
        assertTrue(result.active());
    }

    @Test
    void throwsWhenFulfillmentNodeDoesNotExist() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        GetFulfillmentNodeByIdService service = new GetFulfillmentNodeByIdService(fulfillmentNodeRepository);
        FulfillmentNodeId missingFulfillmentNodeId = FulfillmentNodeId.random();

        FulfillmentNodeNotFoundException exception = assertThrows(
                FulfillmentNodeNotFoundException.class,
                () -> service.getFulfillmentNodeById(new GetFulfillmentNodeByIdQuery(missingFulfillmentNodeId))
        );

        assertEquals(missingFulfillmentNodeId, exception.getFulfillmentNodeId());
        assertEquals("Fulfillment node was not found", exception.getMessage());
    }
}
