package com.santilugani.fulfillmentorchestrator.fulfillment.infrastructure.persistence;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "fulfillment_nodes")
public class FulfillmentNodeJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_daily_capacity", nullable = false)
    private int maxDailyCapacity;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected FulfillmentNodeJpaEntity() {
    }

    private FulfillmentNodeJpaEntity(UUID id, String code, String name, int maxDailyCapacity, boolean active) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.maxDailyCapacity = maxDailyCapacity;
        this.active = active;
    }

    public static FulfillmentNodeJpaEntity fromDomain(FulfillmentNode fulfillmentNode) {
        Objects.requireNonNull(fulfillmentNode, "fulfillmentNode must not be null");

        return new FulfillmentNodeJpaEntity(
                fulfillmentNode.getId().value(),
                fulfillmentNode.getCode(),
                fulfillmentNode.getName(),
                fulfillmentNode.getMaxDailyCapacity(),
                fulfillmentNode.isActive()
        );
    }

    public FulfillmentNode toDomain() {
        return FulfillmentNode.reconstitute(
                new FulfillmentNodeId(id),
                code,
                name,
                maxDailyCapacity,
                active
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }
}
