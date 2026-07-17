package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

public enum FanLevel implements ProfileOption {
    BEGINNER("beginner", "입문", "", true),
    LIGHT("light", "라이트팬", "", true),
    CORE("core", "코어팬", "", true);

    public static final boolean MULTI_SELECTABLE = false;
    public static final Integer MAX_COUNT = null;

    private final String value;
    private final String label;
    private final String description;
    private final boolean active;

    FanLevel(String value, String label, String description, boolean active) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.active = active;
    }

    @JsonCreator
    public static FanLevel from(String value) {
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

    public static List<FanLevel> activeValues() {
        return Arrays.stream(values()).filter(FanLevel::active).toList();
    }
}
