package com.sportsmate.server.domain.application.matching.filter;

import com.sportsmate.server.domain.application.matching.MatchCandidate;
import org.springframework.stereotype.Component;

@Component
public class NotPreviouslyRejectedFilter implements MatchFilter {

    @Override
    public boolean isEligible(MatchCandidate a, MatchCandidate b) {
        return !a.rejectedMemberIds().contains(b.memberId())
                && !b.rejectedMemberIds().contains(a.memberId());
    }
}
