package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class CreateFulfillmentNodeService implements CreateFulfillmentNodeUseCase {

    private final FulfillmentNodeRepository fulfillmentNodeRepository;

    public CreateFulfillmentNodeService(FulfillmentNodeRepository fulfillmentNodeRepository) {
        this.fulfillmentNodeRepository = Objects.requireNonNull(
                fulfillmentNodeRepository,
                "fulfillmentNodeRepository must not be null"
        );
    }

    @Override
    public FulfillmentNodeResult createFulfillmentNode(CreateFulfillmentNodeCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String normalizedCode = command.code().trim();
        if (fulfillmentNodeRepository.existsByCode(normalizedCode)) {
            throw new FulfillmentNodeCodeAlreadyExistsException(normalizedCode);
        }

        FulfillmentNode fulfillmentNode = new FulfillmentNode(
                FulfillmentNodeId.random(),
                normalizedCode,
                command.name(),
                command.maxDailyCapacity()
        );

        fulfillmentNodeRepository.save(fulfillmentNode);
        return FulfillmentNodeResult.from(fulfillmentNode);
    }
}
