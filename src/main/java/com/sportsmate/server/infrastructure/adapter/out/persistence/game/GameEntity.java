package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "games")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    @Column(name = "home_team_id", nullable = false)
    private Long homeTeamId;

    @Column(name = "away_team_id", nullable = false)
    private Long awayTeamId;

    @Column(name = "stadium_id", nullable = false)
    private Long stadiumId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "kbo_game_id", length = 20, unique = true)
    private String kboGameId;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    public boolean updateFrom(GameSyncValues values, LocalDateTime syncedAt) {
        boolean changed = false;
        changed |= updateLeagueId(values.leagueId());
        changed |= updateHomeTeamId(values.homeTeamId());
        changed |= updateAwayTeamId(values.awayTeamId());
        changed |= updateStadiumId(values.stadiumId());
        changed |= updateDate(values.date());
        changed |= updateTime(values.time());
        changed |= updateDeadline(values.deadline());
        changed |= updateStatus(values.status());
        changed |= updateHomeScore(values.homeScore());
        changed |= updateAwayScore(values.awayScore());
        this.lastSyncedAt = syncedAt;
        return changed;
    }

    public void cancel(LocalDateTime syncedAt) {
        this.status = "CANCELLED";
        this.lastSyncedAt = syncedAt;
    }

    public record GameSyncValues(
            Long leagueId,
            Long homeTeamId,
            Long awayTeamId,
            Long stadiumId,
            LocalDate date,
            LocalTime time,
            LocalDate deadline,
            String status,
            Integer homeScore,
            Integer awayScore
    ) {
    }

    private boolean updateLeagueId(Long value) {
        if (java.util.Objects.equals(leagueId, value)) return false;
        leagueId = value;
        return true;
    }

    private boolean updateHomeTeamId(Long value) {
        if (java.util.Objects.equals(homeTeamId, value)) return false;
        homeTeamId = value;
        return true;
    }

    private boolean updateAwayTeamId(Long value) {
        if (java.util.Objects.equals(awayTeamId, value)) return false;
        awayTeamId = value;
        return true;
    }

    private boolean updateStadiumId(Long value) {
        if (java.util.Objects.equals(stadiumId, value)) return false;
        stadiumId = value;
        return true;
    }

    private boolean updateDate(LocalDate value) {
        if (java.util.Objects.equals(date, value)) return false;
        date = value;
        return true;
    }

    private boolean updateTime(LocalTime value) {
        if (java.util.Objects.equals(time, value)) return false;
        time = value;
        return true;
    }

    private boolean updateDeadline(LocalDate value) {
        if (java.util.Objects.equals(deadline, value)) return false;
        deadline = value;
        return true;
    }

    private boolean updateStatus(String value) {
        if (java.util.Objects.equals(status, value)) return false;
        status = value;
        return true;
    }

    private boolean updateHomeScore(Integer value) {
        if (java.util.Objects.equals(homeScore, value)) return false;
        homeScore = value;
        return true;
    }

    private boolean updateAwayScore(Integer value) {
        if (java.util.Objects.equals(awayScore, value)) return false;
        awayScore = value;
        return true;
    }
}
