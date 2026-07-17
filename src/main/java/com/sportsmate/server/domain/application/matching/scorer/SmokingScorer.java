package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import org.springframework.stereotype.Component;

@Component
public class SmokingScorer implements MatchScorer {

    @Override
    public String key() {
        return "smoking";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        return (score(a.smokingPref(), b.smokingStatus())
                + score(b.smokingPref(), a.smokingStatus())) / 2.0;
    }

    private double score(SmokingPref preference, SmokingStatus opponentStatus) {
        if (preference == null || preference == SmokingPref.ANY || opponentStatus == null) {
            return 0.7;
        }
        boolean matches = switch (preference) {
            case SMOKER -> opponentStatus == SmokingStatus.SMOKER;
            case NON_SMOKER -> opponentStatus == SmokingStatus.NON_SMOKER;
            case ANY -> true;
        };
        return matches ? 1.0 : 0.2;
    }
}
