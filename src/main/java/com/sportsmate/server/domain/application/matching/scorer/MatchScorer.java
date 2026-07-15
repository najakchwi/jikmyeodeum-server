package com.sportsmate.server.domain.application.matching.scorer;

import com.sportsmate.server.domain.application.matching.MatchCandidate;

public interface MatchScorer {
    String key();
    double score(MatchCandidate a, MatchCandidate b);
}
