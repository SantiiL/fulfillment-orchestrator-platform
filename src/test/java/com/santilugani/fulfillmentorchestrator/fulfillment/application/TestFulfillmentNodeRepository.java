package com.santilugani.fulfillmentorchestrator.fulfillment.application;

import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNode;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class TestFulfillmentNodeRepository implements FulfillmentNodeRepository {

    private final Map<FulfillmentNodeId, FulfillmentNode> fulfillmentNodesById = new HashMap<>();
    private final List<FulfillmentNode> savedFulfillmentNodes = new ArrayList<>();
    private int saveCount;

    void store(FulfillmentNode fulfillmentNode) {
        fulfillmentNodesById.put(fulfillmentNode.getId(), fulfillmentNode);
    }

    int saveCount() {
        return saveCount;
    }

    FulfillmentNode storedFulfillmentNode(FulfillmentNodeId fulfillmentNodeId) {
        return fulfillmentNodesById.get(fulfillmentNodeId);
    }

    List<FulfillmentNode> savedFulfillmentNodes() {
        return savedFulfillmentNodes;
    }

    @Override
    public void save(FulfillmentNode fulfillmentNode) {
        saveCount++;
        fulfillmentNodesById.put(fulfillmentNode.getId(), fulfillmentNode);
        savedFulfillmentNodes.add(fulfillmentNode);
    }

    @Override
    public Optional<FulfillmentNode> findById(FulfillmentNodeId fulfillmentNodeId) {
        return Optional.ofNullable(fulfillmentNodesById.get(fulfillmentNodeId));
    }

    @Override
    public List<FulfillmentNode> findAll() {
        return fulfillmentNodesById.values().stream()
                .sorted(Comparator.comparing(FulfillmentNode::getCode))
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return fulfillmentNodesById.values().stream()
                .anyMatch(fulfillmentNode -> fulfillmentNode.getCode().equals(code));
    }
}
