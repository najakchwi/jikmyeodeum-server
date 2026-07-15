package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum WatchStyle implements ProfileOption {
    CHEER("cheer", "열정 응원형", "응원가·구호 다 따라불러요"),
    FOCUS("focus", "몰입 관람형", "조용히 경기에 집중해요"),
    ENJOY("enjoy", "분위기 즐김형", "야구장 분위기 자체가 좋아요"),
    FOOD("food", "먹방 투어형", "먹는 재미로 가요");

    private final String value;
    private final String label;
    private final String description;

    WatchStyle(String value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
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
}
