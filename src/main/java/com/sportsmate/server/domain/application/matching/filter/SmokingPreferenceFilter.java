package com.sportsmate.server.domain.application.matching.filter;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
public class SmokingPreferenceFilter implements MatchFilter {

    @Override
    public boolean isEligible(MatchCandidate a, MatchCandidate b) {
        return allows(a.smokingPref(), b.smokingStatus())
                && allows(b.smokingPref(), a.smokingStatus());
    }

    private boolean allows(SmokingPref preference, SmokingStatus opponentStatus) {
        if (preference == null || preference == SmokingPref.ANY || opponentStatus == null) {
            return true;
        }
        return switch (preference) {
            case SMOKER -> opponentStatus == SmokingStatus.SMOKER;
            case NON_SMOKER -> opponentStatus == SmokingStatus.NON_SMOKER;
            case ANY -> true;
        };
    }
}
