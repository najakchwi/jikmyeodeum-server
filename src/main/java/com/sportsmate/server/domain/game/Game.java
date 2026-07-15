package com.sportsmate.server.domain.game;

import java.time.LocalDate;
import java.time.LocalTime;

public record Game(
        String id, String homeTeam, String awayTeam, String stadium, LocalDate date,
        LocalTime time, LocalDate deadline, Integer homeScore, Integer awayScore,
        String homeTeamEmblemUrl, String awayTeamEmblemUrl, Long homeTeamId, Long awayTeamId) {

    public String status(boolean applied, LocalDate today) {
        if (applied) return "applied";
        if (today.isBefore(date.minusDays(14))) return "upcoming";
        if (today.isAfter(deadline)) return "closed";
        return "open";
    }
}
