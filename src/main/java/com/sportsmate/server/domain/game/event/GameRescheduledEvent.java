package com.sportsmate.server.domain.game.event;

import com.sportsmate.server.common.domain.Event;
import java.time.LocalDate;
import java.time.LocalTime;

public final class GameRescheduledEvent extends Event {

    private final String gameId;
    private final LocalDate previousDate;
    private final LocalTime previousTime;
    private final LocalDate newDate;
    private final LocalTime newTime;

    public GameRescheduledEvent(
            String gameId,
            LocalDate previousDate,
            LocalTime previousTime,
            LocalDate newDate,
            LocalTime newTime
    ) {
        this.gameId = gameId;
        this.previousDate = previousDate;
        this.previousTime = previousTime;
        this.newDate = newDate;
        this.newTime = newTime;
    }

    public String getGameId() {
        return gameId;
    }

    public LocalDate getPreviousDate() {
        return previousDate;
    }

    public LocalTime getPreviousTime() {
        return previousTime;
    }

    public LocalDate getNewDate() {
        return newDate;
    }

    public LocalTime getNewTime() {
        return newTime;
    }
}
