package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class GetFulfillmentNodeWorkingDaysService implements GetFulfillmentNodeWorkingDaysUseCase {

    private final FulfillmentNodeRepository fulfillmentNodeRepository;

    public GetFulfillmentNodeWorkingDaysService(FulfillmentNodeRepository fulfillmentNodeRepository) {
        this.fulfillmentNodeRepository = Objects.requireNonNull(
                fulfillmentNodeRepository,
                "fulfillmentNodeRepository must not be null"
        );
    }

    @Override
    public FulfillmentNodeWorkingDaysResult getWorkingDays(GetFulfillmentNodeWorkingDaysQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        FulfillmentNode fulfillmentNode = fulfillmentNodeRepository.findById(query.fulfillmentNodeId())
                .orElseThrow(() -> new FulfillmentNodeNotFoundException(query.fulfillmentNodeId()));

        return FulfillmentNodeWorkingDaysResult.from(fulfillmentNode);
    }
}
