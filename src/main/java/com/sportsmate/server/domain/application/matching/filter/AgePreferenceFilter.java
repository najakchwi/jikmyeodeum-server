package com.sportsmate.server.domain.application.matching.filter;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.AgePref;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.stereotype.Component;

@Component
public class AgePreferenceFilter implements MatchFilter {

    private static final int SIMILAR_AGE_RANGE = 3;

    @Override
    public boolean isEligible(MatchCandidate a, MatchCandidate b) {
        return allows(a.agePref(), a.birthdate(), b.birthdate())
                && allows(b.agePref(), b.birthdate(), a.birthdate());
    }

    private boolean allows(AgePref preference, LocalDate ownerBirthdate, LocalDate opponentBirthdate) {
        if (preference == null || preference == AgePref.ANY || ownerBirthdate == null || opponentBirthdate == null) {
            return true;
        }
        int ownerAge = age(ownerBirthdate);
        int opponentAge = age(opponentBirthdate);
        return switch (preference) {
            case SIMILAR -> Math.abs(ownerAge - opponentAge) <= SIMILAR_AGE_RANGE;
            case OLDER -> opponentAge > ownerAge;
            case YOUNGER -> opponentAge < ownerAge;
            case ANY -> true;
        };
    }

    private int age(LocalDate birthdate) {
        return Period.between(birthdate, LocalDate.now()).getYears();
    }
}
