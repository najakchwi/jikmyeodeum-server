package com.sportsmate.server.domain.application.matching;

import java.util.List;

public record MatchPair(
        String applicationAId,
        String applicationBId,
        Long memberAId,
        Long memberBId,
        int score,
        List<MatchReason> reasons) {

    public MatchPair {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
