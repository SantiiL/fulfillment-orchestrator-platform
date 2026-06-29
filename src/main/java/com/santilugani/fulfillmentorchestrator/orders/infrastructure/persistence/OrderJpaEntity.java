package com.santilugani.fulfillmentorchestrator.orders.infrastructure.persistence;

import com.santilugani.fulfillmentorchestrator.orders.domain.AssignedFulfillmentNodeId;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderStatus;
import com.santilugani.fulfillmentorchestrator.orders.domain.SellerId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "fulfillment_node_id")
    private UUID fulfillmentNodeId;

    @Column(name = "allocated_at")
    private OffsetDateTime allocatedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OrderJpaEntity() {
    }

    private OrderJpaEntity(
            UUID id,
            UUID sellerId,
            OrderStatus status,
            UUID fulfillmentNodeId,
            OffsetDateTime allocatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.fulfillmentNodeId = fulfillmentNodeId;
        this.allocatedAt = allocatedAt;
    }

    public static OrderJpaEntity fromDomain(Order order) {
        return new OrderJpaEntity(
                order.getId().value(),
                order.getSellerId().value(),
                order.getStatus(),
                order.getAssignedFulfillmentNodeId() == null ? null : order.getAssignedFulfillmentNodeId().value(),
                order.getAllocatedAt()
        );
    }

    public OrderJpaEntity updateFromDomain(Order order) {
        Objects.requireNonNull(order, "order must not be null");

        if (!id.equals(order.getId().value())) {
            throw new IllegalArgumentException("Order id does not match persisted entity id");
        }

        sellerId = order.getSellerId().value();
        status = order.getStatus();
        fulfillmentNodeId = order.getAssignedFulfillmentNodeId() == null
                ? null
                : order.getAssignedFulfillmentNodeId().value();
        allocatedAt = order.getAllocatedAt();
        return this;
    }

    public Order toDomain() {
        return Order.reconstitute(
                new OrderId(id),
                new SellerId(sellerId),
                status,
                fulfillmentNodeId == null ? null : new AssignedFulfillmentNodeId(fulfillmentNodeId),
                allocatedAt
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
}
