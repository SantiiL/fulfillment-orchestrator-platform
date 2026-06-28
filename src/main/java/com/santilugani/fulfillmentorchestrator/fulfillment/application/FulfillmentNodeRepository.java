package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;

import java.util.List;
import java.util.Optional;

public interface FulfillmentNodeRepository {

    void save(FulfillmentNode fulfillmentNode);

    Optional<FulfillmentNode> findById(FulfillmentNodeId fulfillmentNodeId);

    List<FulfillmentNode> findAll();

    boolean existsByCode(String code);
}
