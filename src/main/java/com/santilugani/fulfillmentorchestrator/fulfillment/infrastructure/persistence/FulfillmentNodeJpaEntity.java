package com.santilugani.fulfillmentorchestrator.fulfillment.infrastructure.persistence;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "fulfillment_node_working_days",
            joinColumns = @JoinColumn(name = "fulfillment_node_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private Set<DayOfWeek> workingDays = EnumSet.noneOf(DayOfWeek.class);

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected FulfillmentNodeJpaEntity() {
    }

    private FulfillmentNodeJpaEntity(
            UUID id,
            String code,
            String name,
            int maxDailyCapacity,
            boolean active,
            Set<DayOfWeek> workingDays
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.maxDailyCapacity = maxDailyCapacity;
        this.active = active;
        this.workingDays = normalizeWorkingDays(workingDays);
    }

    public static FulfillmentNodeJpaEntity fromDomain(FulfillmentNode fulfillmentNode) {
        Objects.requireNonNull(fulfillmentNode, "fulfillmentNode must not be null");

        return new FulfillmentNodeJpaEntity(
                fulfillmentNode.getId().value(),
                fulfillmentNode.getCode(),
                fulfillmentNode.getName(),
                fulfillmentNode.getMaxDailyCapacity(),
                fulfillmentNode.isActive(),
                fulfillmentNode.getWorkingDays()
        );
    }

    public FulfillmentNodeJpaEntity updateFromDomain(FulfillmentNode fulfillmentNode) {
        Objects.requireNonNull(fulfillmentNode, "fulfillmentNode must not be null");

        if (!id.equals(fulfillmentNode.getId().value())) {
            throw new IllegalArgumentException("Fulfillment node id does not match persisted entity id");
        }

        code = fulfillmentNode.getCode();
        name = fulfillmentNode.getName();
        maxDailyCapacity = fulfillmentNode.getMaxDailyCapacity();
        active = fulfillmentNode.isActive();
        workingDays.clear();
        workingDays.addAll(fulfillmentNode.getWorkingDays());
        return this;
    }

    public FulfillmentNode toDomain() {
        return FulfillmentNode.reconstitute(
                new FulfillmentNodeId(id),
                code,
                name,
                maxDailyCapacity,
                active,
                normalizeWorkingDays(workingDays)
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

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static Set<DayOfWeek> normalizeWorkingDays(Set<DayOfWeek> workingDays) {
        Objects.requireNonNull(workingDays, "workingDays must not be null");

        if (workingDays.isEmpty()) {
            return EnumSet.allOf(DayOfWeek.class);
        }

        return EnumSet.copyOf(workingDays);
    }
}
