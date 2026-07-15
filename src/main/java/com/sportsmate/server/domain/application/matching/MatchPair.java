package com.sportsmate.server.domain.application.matching;

public record MatchPair(
        String applicationAId,
        String applicationBId,
        Long memberAId,
        Long memberBId,
        int score) {
}
