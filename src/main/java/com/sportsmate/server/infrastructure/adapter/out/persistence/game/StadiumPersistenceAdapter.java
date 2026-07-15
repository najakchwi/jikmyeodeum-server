package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.game.Stadium;
import com.sportsmate.server.domain.game.port.out.StadiumOutPort;
import java.util.Optional;

@PersistenceAdapter
public class StadiumPersistenceAdapter implements StadiumOutPort {

    private final StadiumJpaRepository repository;

    public StadiumPersistenceAdapter(StadiumJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Stadium> findByKboCode(String kboCode) {
        return repository.findByKboCode(kboCode)
                .map(entity -> new Stadium(entity.getId(), entity.getName()));
    }
}
