package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.FanLevelPref;
import org.springframework.stereotype.Component;

@Component
public class FanLevelScorer implements MatchScorer {

    @Override
    public String key() {
        return "fanLevel";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        if (a.fanLevel() == null || b.fanLevel() == null) {
            return 0.5;
        }
        boolean same = a.fanLevel() == b.fanLevel();
        return (score(a.fanLevelPref(), same) + score(b.fanLevelPref(), same)) / 2.0;
    }

    private double score(FanLevelPref preference, boolean same) {
        if (preference == FanLevelPref.SIMILAR) {
            return same ? 1.0 : 0.35;
        }
        return same ? 0.8 : 0.65;
    }
}
