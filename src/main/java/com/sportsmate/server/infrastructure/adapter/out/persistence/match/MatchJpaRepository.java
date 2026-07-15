package com.sportsmate.server.infrastructure.adapter.out.persistence.match;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchJpaRepository extends JpaRepository<MatchEntity, Long> {
}
