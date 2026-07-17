package com.sportsmate.server.domain.application.matching.filter;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.GenderPref;
public class GenderPreferenceFilter implements MatchFilter {

    @Override
    public boolean isEligible(MatchCandidate a, MatchCandidate b) {
        return allows(a.genderPref(), a.gender(), b.gender())
                && allows(b.genderPref(), b.gender(), a.gender());
    }

    private boolean allows(GenderPref preference, String ownerGender, String opponentGender) {
        if (preference == null || preference == GenderPref.ANY || ownerGender == null || opponentGender == null) {
            return true;
        }
        boolean same = ownerGender.equalsIgnoreCase(opponentGender);
        return preference == GenderPref.SAME ? same : !same;
    }
}
