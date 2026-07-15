package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberWithdrawalLogJpaRepository extends JpaRepository<MemberWithdrawalLogEntity, Long> {
    Optional<MemberWithdrawalLogEntity> findByMemberId(Long memberId);
}
