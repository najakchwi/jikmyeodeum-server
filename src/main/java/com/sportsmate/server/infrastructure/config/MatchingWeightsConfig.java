package com.sportsmate.server.infrastructure.config;

import com.sportsmate.server.domain.application.matching.MatchWeights;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchingWeightsConfig {

    @Bean
    @ConfigurationProperties(prefix = "matching")
    MatchingProperties matchingProperties() {
        return new MatchingProperties();
    }

    @Bean
    MatchWeights matchWeights(MatchingProperties properties) {
        return new MatchWeights(properties.getWeights(), properties.getMinimumScore());
    }

    public static class MatchingProperties {
        private Map<String, Double> weights = Map.of();
        private int minimumScore = 60;

        public Map<String, Double> getWeights() {
            return weights;
        }

        public void setWeights(Map<String, Double> weights) {
            this.weights = weights == null ? Map.of() : Map.copyOf(weights);
        }

        public int getMinimumScore() {
            return minimumScore;
        }

        public void setMinimumScore(int minimumScore) {
            this.minimumScore = minimumScore;
        }
    }
}
