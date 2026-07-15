package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MemberLocationVerificationJpaRepository
        extends JpaRepository<MemberLocationVerificationEntity, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM MemberLocationVerificationEntity l WHERE l.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
