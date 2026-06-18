package com.santilugani.fulfillmentorchestrator.orders.infrastructure.persistence;

import com.santilugani.fulfillmentorchestrator.orders.application.OrderRepository;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import com.santilugani.fulfillmentorchestrator.orders.domain.OrderId;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository springDataOrderRepository;

    public JpaOrderRepositoryAdapter(SpringDataOrderRepository springDataOrderRepository) {
        this.springDataOrderRepository = Objects.requireNonNull(
                springDataOrderRepository,
                "springDataOrderRepository must not be null"
        );
    }

    @Override
    public void save(Order order) {
        OrderJpaEntity entity = springDataOrderRepository.findById(order.getId().value())
                .map(existingEntity -> existingEntity.updateFromDomain(order))
                .orElseGet(() -> OrderJpaEntity.fromDomain(order));

        springDataOrderRepository.save(entity);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return springDataOrderRepository.findById(orderId.value())
                .map(OrderJpaEntity::toDomain);
    }
}
