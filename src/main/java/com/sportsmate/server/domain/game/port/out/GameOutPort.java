package com.sportsmate.server.domain.game.port.out;

import com.sportsmate.server.domain.game.event.GameRescheduledEvent;
import com.sportsmate.server.domain.game.Game;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface GameOutPort {
    List<Game> findBetween(LocalDate startDate, LocalDate endDate);
    Optional<SeasonRange> findSeasonRange();
    List<Game> findByDate(LocalDate date);
    Optional<Game> findById(String id);
    long countApplications(String gameId);
    long countWaitingApplications(String gameId);
    UpsertResult upsertAll(List<GameSyncCommand> commands);
    int cancelMissingSyncedGames(YearMonth month, List<String> fetchedKboGameIds);

    record GameSyncCommand(
            String kboGameId,
            Long leagueId,
            Long homeTeamId,
            Long awayTeamId,
            Long stadiumId,
            LocalDate date,
            LocalTime time,
            String status,
            Integer homeScore,
            Integer awayScore
    ) {
    }

    record UpsertResult(
            int created,
            int updated,
            List<GameRescheduledEvent> rescheduledEvents
    ) {
    }

    record SeasonRange(LocalDate startDate, LocalDate endDate) {
    }
}
