package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MemberPreferenceJpaRepository extends JpaRepository<MemberPreferenceEntity, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM MemberPreferenceEntity p WHERE p.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
