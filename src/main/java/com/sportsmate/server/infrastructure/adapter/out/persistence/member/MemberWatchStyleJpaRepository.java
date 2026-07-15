package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MemberWatchStyleJpaRepository extends JpaRepository<MemberWatchStyleEntity, MemberWatchStyleId> {

    @Query("SELECT w FROM MemberWatchStyleEntity w WHERE w.id.memberId = :memberId")
    List<MemberWatchStyleEntity> findAllByMemberId(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM MemberWatchStyleEntity w WHERE w.id.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
