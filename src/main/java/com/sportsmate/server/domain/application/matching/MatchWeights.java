package com.sportsmate.server.domain.application.matching;

import java.util.Map;

public record MatchWeights(Map<String, Double> values, int minimumScore) {

    public MatchWeights {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public double weightFor(String key) {
        return values.getOrDefault(key, 0.0);
    }
}
