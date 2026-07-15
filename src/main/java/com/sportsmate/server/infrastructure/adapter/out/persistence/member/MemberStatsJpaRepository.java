package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberStatsJpaRepository extends JpaRepository<MemberStatsEntity, Long> {
}
