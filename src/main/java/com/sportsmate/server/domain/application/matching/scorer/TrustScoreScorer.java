package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import org.springframework.stereotype.Component;

@Component
public class TrustScoreScorer implements CoreMatchScorer {

    @Override
    public String key() {
        return "trust";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        return clamp((a.trustScore() + b.trustScore()) / 200.0);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
