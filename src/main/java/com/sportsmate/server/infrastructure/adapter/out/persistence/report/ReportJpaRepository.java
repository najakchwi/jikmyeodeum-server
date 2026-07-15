package com.sportsmate.server.infrastructure.adapter.out.persistence.report;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportJpaRepository extends JpaRepository<ReportEntity, Long> {
    boolean existsByReporterIdAndTargetMemberIdAndMatchId(Long reporterId, Long targetMemberId, Long matchId);
}
