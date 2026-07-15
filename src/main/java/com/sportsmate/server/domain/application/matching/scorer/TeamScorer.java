package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import org.springframework.stereotype.Component;

@Component
public class TeamScorer implements MatchScorer {

    @Override
    public String key() {
        return "team";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        if (a.team() == null || b.team() == null) {
            return 0.5;
        }
        return a.team().equalsIgnoreCase(b.team()) ? 1.0 : 0.0;
    }
}
