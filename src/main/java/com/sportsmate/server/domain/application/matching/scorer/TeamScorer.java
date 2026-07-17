package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.TeamPref;
import org.springframework.stereotype.Component;

@Component
public class TeamScorer implements MatchScorer {

    @Override
    public String key() {
        return "team";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        if (a.favoriteTeamId() == null || b.favoriteTeamId() == null) {
            if (a.team() == null || b.team() == null) {
                return 0.5;
            }
            return a.team().equalsIgnoreCase(b.team()) ? 1.0 : 0.0;
        }
        boolean sameTeam = a.favoriteTeamId().equals(b.favoriteTeamId());
        double aScore = preferenceScore(a.teamPref(), sameTeam);
        double bScore = preferenceScore(b.teamPref(), sameTeam);
        return (aScore + bScore) / 2.0;
    }

    private double preferenceScore(TeamPref preference, boolean sameTeam) {
        if (preference == TeamPref.SAME) {
            return sameTeam ? 1.0 : 0.2;
        }
        return sameTeam ? 0.8 : 0.6;
    }
}
