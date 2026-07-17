package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.game.League;
import com.sportsmate.server.domain.game.port.out.LeagueOutPort;
import java.util.List;

@PersistenceAdapter
public class LeaguePersistenceAdapter implements LeagueOutPort {

    private final LeagueJpaRepository repository;

    public LeaguePersistenceAdapter(LeagueJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<League> findAll() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(entity -> new League(
                        entity.getId(),
                        entity.getSportId(),
                        sportName(entity.getSportId()),
                        entity.getCode(),
                        entity.getName()))
                .toList();
    }

    private String sportName(Long sportId) {
        return Long.valueOf(1L).equals(sportId) ? "baseball" : String.valueOf(sportId);
    }
}
