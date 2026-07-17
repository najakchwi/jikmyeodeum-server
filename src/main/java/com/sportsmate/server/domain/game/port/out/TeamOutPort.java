package com.sportsmate.server.domain.game.port.out;

import com.sportsmate.server.domain.game.Team;
import java.util.List;
import java.util.Optional;

public interface TeamOutPort {

    List<Team> findAll();
    default List<Team> findByLeagueId(Long leagueId) {
        return findAll().stream()
                .filter(team -> leagueId.equals(team.leagueId()))
                .toList();
    }
    Optional<Team> findByKboCode(String kboCode);
    Optional<Team> findByShortName(String shortName);
    default boolean existsByIdAndLeagueId(Long teamId, Long leagueId) {
        return teamId == null || findAll().stream()
                .anyMatch(team -> team.id().equals(teamId) && leagueId.equals(team.leagueId()));
    }
}
