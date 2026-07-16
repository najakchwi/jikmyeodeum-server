package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

public enum TalkStyle implements ProfileOption {
    TALKATIVE("talkative", "말이 많은 편", "", true),
    MODERATE("moderate", "적당히", "", true),
    QUIET("quiet", "조용한 편", "", true);

    public static final boolean MULTI_SELECTABLE = false;
    public static final Integer MAX_COUNT = null;

    private final String value;
    private final String label;
    private final String description;
    private final boolean active;

    TalkStyle(String value, String label, String description, boolean active) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.active = active;
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
    @Override public boolean active() { return active; }

    public static List<TalkStyle> activeValues() {
        return Arrays.stream(values())
                .filter(TalkStyle::active)
                .toList();
    }
}
