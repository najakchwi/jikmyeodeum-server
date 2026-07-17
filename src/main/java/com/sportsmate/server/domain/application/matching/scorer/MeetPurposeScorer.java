package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import org.springframework.stereotype.Component;

@Component
public class MeetPurposeScorer implements MatchScorer {

    @Override
    public String key() {
        return "meetPurpose";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        if (a.meetPurpose() == null || b.meetPurpose() == null) {
            return 0.5;
        }
        return a.meetPurpose() == b.meetPurpose() ? 1.0 : 0.55;
    }
}
