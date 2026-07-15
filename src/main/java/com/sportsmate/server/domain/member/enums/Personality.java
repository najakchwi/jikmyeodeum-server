package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum Personality implements ProfileOption {
    TENSION("tension", "텐션 메이커", "분위기 띄우는 거 좋아해요"),
    CALM("calm", "차분한 동행자", "편안하게 함께해요"),
    PLANNER("planner", "계획형", "약속·일정 딱딱 지켜요"),
    SPONTANEOUS("spontaneous", "즉흥형", "그때그때 자유롭게 만나요");

    private final String value;
    private final String label;
    private final String description;

    Personality(String value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    @JsonCreator
    public static Personality from(String value) {
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
