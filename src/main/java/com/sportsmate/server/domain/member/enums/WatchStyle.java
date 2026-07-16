package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

public enum WatchStyle implements ProfileOption {
    CHEER("cheer", "열정 응원형", "응원가·구호 다 따라불러요", true),
    FOCUS("focus", "몰입 관람형", "조용히 경기에 집중해요", true),
    ENJOY("enjoy", "분위기 즐김형", "야구장 분위기 자체가 좋아요", true),
    FOOD("food", "먹방 투어형", "먹는 재미로 가요", true);

    public static final boolean MULTI_SELECTABLE = true;
    public static final Integer MAX_COUNT = 2;

    private final String value;
    private final String label;
    private final String description;
    private final boolean active;

    WatchStyle(String value, String label, String description, boolean active) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.active = active;
    }

    @JsonCreator
    public static WatchStyle from(String value) {
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

    public static List<WatchStyle> activeValues() {
        return Arrays.stream(values())
                .filter(WatchStyle::active)
                .toList();
    }
}
