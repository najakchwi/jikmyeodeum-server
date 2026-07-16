package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

public enum Personality implements ProfileOption {
    TENSION("tension", "텐션 메이커", "분위기 띄우는 거 좋아해요", true),
    CALM("calm", "차분한 동행자", "편안하게 함께해요", true),
    PLANNER("planner", "계획형", "약속·일정 딱딱 지켜요", true),
    SPONTANEOUS("spontaneous", "즉흥형", "그때그때 자유롭게 만나요", true);

    public static final boolean MULTI_SELECTABLE = false;
    public static final Integer MAX_COUNT = null;

    private final String value;
    private final String label;
    private final String description;
    private final boolean active;

    Personality(String value, String label, String description, boolean active) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.active = active;
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
    @Override public boolean active() { return active; }

    public static List<Personality> activeValues() {
        return Arrays.stream(values())
                .filter(Personality::active)
                .toList();
    }
}
