package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class ReplaceFulfillmentNodeWorkingDaysService implements ReplaceFulfillmentNodeWorkingDaysUseCase {

    private final FulfillmentNodeRepository fulfillmentNodeRepository;

    public ReplaceFulfillmentNodeWorkingDaysService(FulfillmentNodeRepository fulfillmentNodeRepository) {
        this.fulfillmentNodeRepository = Objects.requireNonNull(
                fulfillmentNodeRepository,
                "fulfillmentNodeRepository must not be null"
        );
    }

    @Override
    public FulfillmentNodeWorkingDaysResult replaceWorkingDays(ReplaceFulfillmentNodeWorkingDaysCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        FulfillmentNode fulfillmentNode = fulfillmentNodeRepository.findById(command.fulfillmentNodeId())
                .orElseThrow(() -> new FulfillmentNodeNotFoundException(command.fulfillmentNodeId()));

        fulfillmentNode.replaceWorkingDays(command.workingDays());
        fulfillmentNodeRepository.save(fulfillmentNode);
        return FulfillmentNodeWorkingDaysResult.from(fulfillmentNode);
    }
}
