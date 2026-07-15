package com.sportsmate.server.domain.application.matching;

import java.util.List;

public interface MatchingEngine {
    List<MatchPair> match(List<MatchCandidate> candidates, MatchWeights weights);
}
