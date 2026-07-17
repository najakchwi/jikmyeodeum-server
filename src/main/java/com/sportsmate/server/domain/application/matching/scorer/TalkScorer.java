package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.member.enums.TalkPref;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import org.springframework.stereotype.Component;

@Component
public class TalkScorer implements MatchScorer {

    @Override
    public String key() {
        return "talk";
    }

    @Override
    public double score(MatchCandidate a, MatchCandidate b) {
        if (a.talkStyle() == null || b.talkStyle() == null) {
            return 0.5;
        }
        return (score(a.talkPref(), a.talkStyle(), b.talkStyle())
                + score(b.talkPref(), b.talkStyle(), a.talkStyle())) / 2.0;
    }

    private double score(TalkPref preference, TalkStyle ownerStyle, TalkStyle opponentStyle) {
        if (preference == TalkPref.TALKATIVE) {
            return opponentStyle == TalkStyle.TALKATIVE ? 1.0 : 0.45;
        }
        if (preference == TalkPref.QUIET) {
            return opponentStyle == TalkStyle.QUIET ? 1.0 : 0.45;
        }
        return ownerStyle == opponentStyle ? 0.85 : 0.65;
    }
}
