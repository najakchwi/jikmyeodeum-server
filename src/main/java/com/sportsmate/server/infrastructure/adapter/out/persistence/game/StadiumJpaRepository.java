package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumJpaRepository extends JpaRepository<StadiumEntity, Long> {
    Optional<StadiumEntity> findByKboCode(String kboCode);
}
