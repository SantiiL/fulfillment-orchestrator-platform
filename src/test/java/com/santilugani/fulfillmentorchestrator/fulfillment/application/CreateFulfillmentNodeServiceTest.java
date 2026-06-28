package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateFulfillmentNodeServiceTest {

    @Test
    void createsActiveFulfillmentNodeWithGeneratedId() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        CreateFulfillmentNodeService service = new CreateFulfillmentNodeService(fulfillmentNodeRepository);

        FulfillmentNodeResult result = service.createFulfillmentNode(
                new CreateFulfillmentNodeCommand("AR-BUE-01", "Buenos Aires Node 1", 100)
        );

        assertNotNull(result.id());
        assertEquals("AR-BUE-01", result.code());
        assertEquals("Buenos Aires Node 1", result.name());
        assertEquals(100, result.maxDailyCapacity());
        assertTrue(result.active());
    }

    @Test
    void persistsCreatedFulfillmentNode() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        CreateFulfillmentNodeService service = new CreateFulfillmentNodeService(fulfillmentNodeRepository);

        FulfillmentNodeResult result = service.createFulfillmentNode(
                new CreateFulfillmentNodeCommand("AR-BUE-01", "Buenos Aires Node 1", 100)
        );

        assertEquals(1, fulfillmentNodeRepository.saveCount());
        assertEquals(1, fulfillmentNodeRepository.savedFulfillmentNodes().size());

        FulfillmentNode savedFulfillmentNode = fulfillmentNodeRepository.savedFulfillmentNodes().getFirst();
        assertEquals(result.id(), savedFulfillmentNode.getId().value());
        assertEquals("AR-BUE-01", savedFulfillmentNode.getCode());
        assertEquals("Buenos Aires Node 1", savedFulfillmentNode.getName());
        assertEquals(100, savedFulfillmentNode.getMaxDailyCapacity());
        assertTrue(savedFulfillmentNode.isActive());
    }

    @Test
    void throwsWhenCodeAlreadyExists() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        CreateFulfillmentNodeService service = new CreateFulfillmentNodeService(fulfillmentNodeRepository);
        fulfillmentNodeRepository.store(
                new FulfillmentNode(
                        com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId.random(),
                        "AR-BUE-01",
                        "Buenos Aires Node 1",
                        100
                )
        );

        FulfillmentNodeCodeAlreadyExistsException exception = assertThrows(
                FulfillmentNodeCodeAlreadyExistsException.class,
                () -> service.createFulfillmentNode(
                        new CreateFulfillmentNodeCommand("AR-BUE-01", "Buenos Aires Node 2", 150)
                )
        );

        assertEquals("Fulfillment node code already exists", exception.getMessage());
        assertEquals("AR-BUE-01", exception.getCode());
        assertEquals(0, fulfillmentNodeRepository.saveCount());
    }
}
