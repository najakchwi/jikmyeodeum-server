package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import org.springframework.stereotype.Component;

@Component
public class PersonalityScorer implements MatchScorer {

    @Override
    public String key() {
        return "personality";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        double personalityScore = a.personality() == null || b.personality() == null
                ? 0.5
                : (a.personality() == b.personality() ? 1.0 : 0.6);
        double talkStyleScore = a.talkStyle() == null || b.talkStyle() == null
                ? 0.5
                : (a.talkStyle() == b.talkStyle() ? 1.0 : 0.6);
        return (personalityScore + talkStyleScore) / 2.0;
    }
}
