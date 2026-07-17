package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.DrinkingPref;
import com.sportsmate.server.domain.member.enums.DrinkingStatus;
import org.springframework.stereotype.Component;

@Component
public class DrinkingScorer implements MatchScorer {

    @Override
    public String key() {
        return "drinking";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        return (score(a.drinkingPref(), b.drinkingStatus())
                + score(b.drinkingPref(), a.drinkingStatus())) / 2.0;
    }

    private double score(DrinkingPref preference, DrinkingStatus opponentStatus) {
        if (preference == null || preference == DrinkingPref.ANY || opponentStatus == null) {
            return 0.7;
        }
        boolean opponentDrinks = opponentStatus == DrinkingStatus.DRINKER
                || opponentStatus == DrinkingStatus.SOMETIMES;
        boolean matches = preference == DrinkingPref.DRINKER ? opponentDrinks : !opponentDrinks;
        return matches ? 1.0 : 0.25;
    }
}
