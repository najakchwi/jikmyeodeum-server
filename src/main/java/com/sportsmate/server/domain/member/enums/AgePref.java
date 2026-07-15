package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum AgePref implements ProfileOption {
    SIMILAR("similar", "또래", ""),
    OLDER("older", "연상", ""),
    YOUNGER("younger", "연하", ""),
    ANY("any", "무관", "");

    private final String value;
    private final String label;
    private final String description;

    AgePref(String value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    @JsonCreator
    public static AgePref from(String value) {
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
