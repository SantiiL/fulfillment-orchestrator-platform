package com.santilugani.fulfillmentorchestrator.orders.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    long countByFulfillmentNodeIdAndAllocatedAtGreaterThanEqualAndAllocatedAtLessThan(
            UUID fulfillmentNodeId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    );
}
