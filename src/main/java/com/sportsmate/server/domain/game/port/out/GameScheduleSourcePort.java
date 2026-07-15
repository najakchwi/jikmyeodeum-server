package com.sportsmate.server.domain.game.port.out;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

public interface GameScheduleSourcePort {

    List<SourceGame> fetchMonthlySchedule(YearMonth month);

    record SourceGame(
            String sourceGameId,
            Long homeTeamId,
            Long awayTeamId,
            Long stadiumId,
            LocalDate date,
            LocalTime time,
            Integer homeScore,
            Integer awayScore
    ) {
    }
}
