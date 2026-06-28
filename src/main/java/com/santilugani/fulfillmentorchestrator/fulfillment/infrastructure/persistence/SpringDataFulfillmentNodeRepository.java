package com.santilugani.fulfillmentorchestrator.fulfillment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataFulfillmentNodeRepository extends JpaRepository<FulfillmentNodeJpaEntity, UUID> {

    boolean existsByCode(String code);

    List<FulfillmentNodeJpaEntity> findAllByOrderByCodeAsc();
}
