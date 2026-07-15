package com.sportsmate.server.domain.game.port.in;

import java.time.LocalDate;
import java.util.List;

public interface GameUseCase {
    CalendarResult calendar(LocalDate startDate, LocalDate endDate);
    List<GameResult> games(Long memberId, LocalDate date, List<String> teams);
    GameResult game(Long memberId, String gameId);

    record CalendarResult(List<LocalDate> dates, LocalDate seasonStartDate, LocalDate seasonEndDate) {}
    record GameResult(
            String id, String homeTeam, String awayTeam, String stadium, LocalDate date,
            String time, LocalDate deadline, String status, long applicantCount,
            Integer homeScore, Integer awayScore, String applicationId,
            String homeTeamEmblemUrl, String awayTeamEmblemUrl) {}
}
