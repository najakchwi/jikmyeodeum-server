package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum SmokingPref implements ProfileOption {
    SMOKER("smoker", "흡연", "흡연자와 함께해도 괜찮아요"),
    NON_SMOKER("non-smoker", "비흡연", "비흡연자와 함께하고 싶어요"),
    ANY("any", "무관", "흡연 여부에 상관없이 매칭해요");

    private final String value;
    private final String label;
    private final String description;

    SmokingPref(String value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    @JsonCreator
    public static SmokingPref from(String value) {
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
    @Override public boolean active() { return true; }
}
