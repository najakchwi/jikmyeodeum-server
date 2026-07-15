package com.sportsmate.server.common.port.out.alert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AlertMessage(
        String title,
        String summary,
        Map<String, String> fields,
        String dedupeKey) {

    public AlertMessage {
        fields = fields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static AlertMessage of(String title, String summary, String dedupeKey) {
        return new AlertMessage(title, summary, Map.of(), dedupeKey);
    }
}
