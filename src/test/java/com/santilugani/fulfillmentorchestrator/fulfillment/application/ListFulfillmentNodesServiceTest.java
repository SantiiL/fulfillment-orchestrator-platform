package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListFulfillmentNodesServiceTest {

    @Test
    void returnsFulfillmentNodesOrderedByCode() {
        TestFulfillmentNodeRepository fulfillmentNodeRepository = new TestFulfillmentNodeRepository();
        ListFulfillmentNodesService service = new ListFulfillmentNodesService(fulfillmentNodeRepository);
        fulfillmentNodeRepository.store(
                new FulfillmentNode(FulfillmentNodeId.random(), "AR-BUE-02", "Buenos Aires Node 2", 200)
        );
        fulfillmentNodeRepository.store(
                new FulfillmentNode(FulfillmentNodeId.random(), "AR-BUE-01", "Buenos Aires Node 1", 100)
        );

        List<FulfillmentNodeResult> result = service.listFulfillmentNodes();

        assertEquals(2, result.size());
        assertEquals("AR-BUE-01", result.getFirst().code());
        assertEquals("AR-BUE-02", result.get(1).code());
        assertTrue(result.stream().allMatch(FulfillmentNodeResult::active));
    }
}
