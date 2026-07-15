package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameJpaRepository extends JpaRepository<GameEntity, Long> {
    List<GameEntity> findByDateBetweenOrderByDateAscTimeAsc(LocalDate start, LocalDate end);
    List<GameEntity> findByDateOrderByTimeAsc(LocalDate date);
    Optional<GameEntity> findFirstByOrderByDateAsc();
    Optional<GameEntity> findFirstByOrderByDateDesc();
    Optional<GameEntity> findByKboGameId(String kboGameId);
    List<GameEntity> findByDateBetweenAndKboGameIdIsNotNull(LocalDate start, LocalDate end);
}
