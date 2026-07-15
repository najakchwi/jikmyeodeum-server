package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.game.Team;
import com.sportsmate.server.domain.game.port.out.TeamOutPort;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
public class TeamPersistenceAdapter implements TeamOutPort {

    private final TeamJpaRepository repository;

    public TeamPersistenceAdapter(TeamJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Team> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(TeamEntity::getId))
                .map(entity -> new Team(
                        entity.getId(),
                        entity.getName(),
                        entity.getShortName(),
                        entity.getEmblemImageKey(),
                        entity.getPrimaryColorHex()))
                .toList();
    }

    @Override
    public Optional<Team> findByKboCode(String kboCode) {
        return repository.findByKboCode(kboCode)
                .or(() -> repository.findByShortName(kboCode))
                .map(this::toDomain);
    }

    @Override
    public Optional<Team> findByShortName(String shortName) {
        return repository.findByShortName(shortName).map(this::toDomain);
    }

    private Team toDomain(TeamEntity entity) {
        return new Team(
                entity.getId(),
                entity.getName(),
                entity.getShortName(),
                entity.getEmblemImageKey(),
                entity.getPrimaryColorHex()
        );
    }
}
