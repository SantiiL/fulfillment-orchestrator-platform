package com.santilugani.fulfillmentorchestrator.fulfillment.domain;

import java.util.Objects;

public final class FulfillmentNode {

    private final FulfillmentNodeId id;
    private final String code;
    private final String name;
    private final int maxDailyCapacity;
    private final boolean active;

    public FulfillmentNode(FulfillmentNodeId id, String code, String name, int maxDailyCapacity) {
        this(id, code, name, maxDailyCapacity, true);
    }

    public static FulfillmentNode reconstitute(
            FulfillmentNodeId id,
            String code,
            String name,
            int maxDailyCapacity,
            boolean active
    ) {
        return new FulfillmentNode(id, code, name, maxDailyCapacity, active);
    }

    private FulfillmentNode(
            FulfillmentNodeId id,
            String code,
            String name,
            int maxDailyCapacity,
            boolean active
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.maxDailyCapacity = requirePositive(maxDailyCapacity, "maxDailyCapacity");
        this.active = active;
    }

    public FulfillmentNodeId getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getMaxDailyCapacity() {
        return maxDailyCapacity;
    }

    public boolean isActive() {
        return active;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");

        String normalizedValue = value.trim();
        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalizedValue;
    }

    private static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }

        return value;
    }
}
