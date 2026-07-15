package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamJpaRepository extends JpaRepository<TeamEntity, Long> {
    Optional<TeamEntity> findByShortName(String shortName);
    Optional<TeamEntity> findByKboCode(String kboCode);
}
