package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.GenderPref;
import org.springframework.stereotype.Component;

@Component
public class GenderScorer implements MatchScorer {

    @Override
    public String key() {
        return "gender";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        return (score(a.genderPref(), a.gender(), b.gender())
                + score(b.genderPref(), b.gender(), a.gender())) / 2.0;
    }

    private double score(GenderPref preference, String ownerGender, String opponentGender) {
        if (preference == null || preference == GenderPref.ANY || ownerGender == null || opponentGender == null) {
            return 0.7;
        }
        boolean same = ownerGender.equalsIgnoreCase(opponentGender);
        boolean matches = preference == GenderPref.SAME ? same : !same;
        return matches ? 1.0 : 0.0;
    }
}
