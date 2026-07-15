package com.sportsmate.server.domain.member.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum Gender {
    MALE,
    FEMALE;

    @JsonCreator
    public static Gender from(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
