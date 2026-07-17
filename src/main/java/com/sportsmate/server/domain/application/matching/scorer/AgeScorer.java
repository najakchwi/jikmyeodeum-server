package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.AgePref;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.stereotype.Component;

@Component
public class AgeScorer implements MatchScorer {
    private static final int SIMILAR_AGE_RANGE = 3;

    @Override
    public String key() {
        return "age";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        return (score(a.agePref(), a.birthdate(), b.birthdate())
                + score(b.agePref(), b.birthdate(), a.birthdate())) / 2.0;
    }

    private double score(AgePref preference, LocalDate ownerBirthdate, LocalDate opponentBirthdate) {
        if (preference == null || preference == AgePref.ANY || ownerBirthdate == null || opponentBirthdate == null) {
            return 0.7;
        }
        int ownerAge = age(ownerBirthdate);
        int opponentAge = age(opponentBirthdate);
        boolean matches = switch (preference) {
            case SIMILAR -> Math.abs(ownerAge - opponentAge) <= SIMILAR_AGE_RANGE;
            case OLDER -> opponentAge > ownerAge;
            case YOUNGER -> opponentAge < ownerAge;
            case ANY -> true;
        };
        return matches ? 1.0 : 0.25;
    }

    private int age(LocalDate birthdate) {
        return Period.between(birthdate, LocalDate.now()).getYears();
    }
}
