package com.santilugani.fulfillmentorchestrator.fulfillment.infrastructure.persistence;

import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeRepository;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JpaFulfillmentNodeRepositoryAdapter implements FulfillmentNodeRepository {

    private final SpringDataFulfillmentNodeRepository springDataFulfillmentNodeRepository;

    public JpaFulfillmentNodeRepositoryAdapter(SpringDataFulfillmentNodeRepository springDataFulfillmentNodeRepository) {
        this.springDataFulfillmentNodeRepository = Objects.requireNonNull(
                springDataFulfillmentNodeRepository,
                "springDataFulfillmentNodeRepository must not be null"
        );
    }

    @Override
    public void save(FulfillmentNode fulfillmentNode) {
        FulfillmentNodeJpaEntity entity = springDataFulfillmentNodeRepository.findById(fulfillmentNode.getId().value())
                .map(existingEntity -> existingEntity.updateFromDomain(fulfillmentNode))
                .orElseGet(() -> FulfillmentNodeJpaEntity.fromDomain(fulfillmentNode));

        springDataFulfillmentNodeRepository.save(entity);
    }

    @Override
    public Optional<FulfillmentNode> findById(FulfillmentNodeId fulfillmentNodeId) {
        return springDataFulfillmentNodeRepository.findById(fulfillmentNodeId.value())
                .map(FulfillmentNodeJpaEntity::toDomain);
    }

    @Override
    public List<FulfillmentNode> findAll() {
        return springDataFulfillmentNodeRepository.findAllByOrderByCodeAsc().stream()
                .map(FulfillmentNodeJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return springDataFulfillmentNodeRepository.existsByCode(code);
    }
}
