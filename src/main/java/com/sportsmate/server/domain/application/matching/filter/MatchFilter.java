package com.sportsmate.server.domain.application.matching.filter;

import com.sportsmate.server.domain.application.matching.MatchCandidate;

public interface MatchFilter {
    boolean isEligible(MatchCandidate a, MatchCandidate b);
}
