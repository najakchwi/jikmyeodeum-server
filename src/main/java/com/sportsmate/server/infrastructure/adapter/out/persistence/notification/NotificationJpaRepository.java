package com.sportsmate.server.infrastructure.adapter.out.persistence.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    long countByMemberIdAndReadFalse(Long memberId);
    Optional<NotificationEntity> findByIdAndMemberId(Long id, Long memberId);
}
