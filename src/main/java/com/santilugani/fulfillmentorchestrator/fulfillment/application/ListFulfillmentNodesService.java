package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ListFulfillmentNodesService implements ListFulfillmentNodesUseCase {

    private final FulfillmentNodeRepository fulfillmentNodeRepository;

    public ListFulfillmentNodesService(FulfillmentNodeRepository fulfillmentNodeRepository) {
        this.fulfillmentNodeRepository = Objects.requireNonNull(
                fulfillmentNodeRepository,
                "fulfillmentNodeRepository must not be null"
        );
    }

    @Override
    public List<FulfillmentNodeResult> listFulfillmentNodes() {
        return fulfillmentNodeRepository.findAll().stream()
                .map(FulfillmentNodeResult::from)
                .toList();
    }
}
