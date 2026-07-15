package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum TalkStyle implements ProfileOption {
    TALKATIVE("talkative", "말이 많은 편", ""),
    MODERATE("moderate", "적당히", ""),
    QUIET("quiet", "조용한 편", "");

    private final String value;
    private final String label;
    private final String description;

    TalkStyle(String value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    @JsonCreator
    public static TalkStyle from(String value) {
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
