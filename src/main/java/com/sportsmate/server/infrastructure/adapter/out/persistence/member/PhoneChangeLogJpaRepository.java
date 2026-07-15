package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneChangeLogJpaRepository extends JpaRepository<PhoneChangeLogEntity, Long> {

    Optional<PhoneChangeLogEntity> findFirstByMemberIdAndChangedAtGreaterThanEqualOrderByChangedAtDesc(
            Long memberId,
            LocalDateTime changedAt);
}
