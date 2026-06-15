package com.santilugani.fulfillmentorchestrator.orders.infrastructure.persistence;

import com.santilugani.fulfillmentorchestrator.orders.application.OrderRepository;
import com.santilugani.fulfillmentorchestrator.orders.domain.Order;
import org.springframework.stereotype.Repository;

import java.util.Objects;

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
        springDataOrderRepository.save(OrderJpaEntity.fromDomain(order));
    }
}
