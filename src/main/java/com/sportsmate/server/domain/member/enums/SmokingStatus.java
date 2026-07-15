package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum SmokingStatus implements ProfileOption {
    NON_SMOKER("non-smoker", "비흡연", ""),
    SMOKER("smoker", "흡연", "");

    private final String value;
    private final String label;
    private final String description;

    SmokingStatus(String value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    @JsonCreator
    public static SmokingStatus from(String value) {
        return Arrays.stream(values())
                .filter(option -> option.value.equals(value))
                .findFirst()
                .orElseGet(() -> valueOf(value.toUpperCase().replace('-', '_')));
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    @Override public String label() { return label; }
    @Override public String description() { return description; }
}
