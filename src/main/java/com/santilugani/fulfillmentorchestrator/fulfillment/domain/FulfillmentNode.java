package com.santilugani.fulfillmentorchestrator.fulfillment.domain;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class FulfillmentNode {

    private final FulfillmentNodeId id;
    private final String code;
    private final String name;
    private final int maxDailyCapacity;
    private final boolean active;
    private Set<DayOfWeek> workingDays;

    public FulfillmentNode(FulfillmentNodeId id, String code, String name, int maxDailyCapacity) {
        this(id, code, name, maxDailyCapacity, true, EnumSet.allOf(DayOfWeek.class));
    }

    public static FulfillmentNode reconstitute(
            FulfillmentNodeId id,
            String code,
            String name,
            int maxDailyCapacity,
            boolean active
    ) {
        return new FulfillmentNode(id, code, name, maxDailyCapacity, active, EnumSet.allOf(DayOfWeek.class));
    }

    public static FulfillmentNode reconstitute(
            FulfillmentNodeId id,
            String code,
            String name,
            int maxDailyCapacity,
            boolean active,
            Set<DayOfWeek> workingDays
    ) {
        return new FulfillmentNode(id, code, name, maxDailyCapacity, active, workingDays);
    }

    private FulfillmentNode(
            FulfillmentNodeId id,
            String code,
            String name,
            int maxDailyCapacity,
            boolean active,
            Set<DayOfWeek> workingDays
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.maxDailyCapacity = requirePositive(maxDailyCapacity, "maxDailyCapacity");
        this.active = active;
        this.workingDays = copyWorkingDays(workingDays);
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

    public Set<DayOfWeek> getWorkingDays() {
        return Collections.unmodifiableSet(EnumSet.copyOf(workingDays));
    }

    public boolean isWorkingDay(DayOfWeek dayOfWeek) {
        Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
        return workingDays.contains(dayOfWeek);
    }

    public void replaceWorkingDays(Set<DayOfWeek> workingDays) {
        this.workingDays = copyWorkingDays(workingDays);
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

    private static Set<DayOfWeek> copyWorkingDays(Set<DayOfWeek> workingDays) {
        Objects.requireNonNull(workingDays, "workingDays must not be null");

        if (workingDays.isEmpty()) {
            throw new IllegalArgumentException("workingDays must not be empty");
        }

        if (workingDays.contains(null)) {
            throw new IllegalArgumentException("workingDays must not contain null");
        }

        return EnumSet.copyOf(workingDays);
    }
}
