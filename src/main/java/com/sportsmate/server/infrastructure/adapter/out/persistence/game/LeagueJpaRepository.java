package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueJpaRepository extends JpaRepository<LeagueEntity, Long> {
    List<LeagueEntity> findAllByOrderByIdAsc();
}
