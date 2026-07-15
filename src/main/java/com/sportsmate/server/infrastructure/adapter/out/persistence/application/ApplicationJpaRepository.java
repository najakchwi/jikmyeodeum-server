package com.sportsmate.server.infrastructure.adapter.out.persistence.application;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationJpaRepository extends JpaRepository<ApplicationEntity, Long> {
    Optional<ApplicationEntity> findByIdAndMemberId(Long id, Long memberId);
    // 취소 후 재신청을 허용하면서 (member, game)에 cancelled + 활성 신청이 공존할 수 있다.
    // 활성 신청은 최대 1개(existsActive가 재신청을 막음)이므로, cancelled를 제외하고 최신 1건만 조회해
    // NonUniqueResultException을 방지한다.
    Optional<ApplicationEntity> findFirstByMemberIdAndGameIdAndStatusNotOrderByAppliedAtDesc(
            Long memberId, Long gameId, String status);
    Optional<ApplicationEntity> findByMatchIdAndMemberId(Long matchId, Long memberId);
    List<ApplicationEntity> findByMemberIdOrderByAppliedAtDesc(Long memberId);

    // 탈퇴 시 활성 신청을 취소/재대기 처리하지만(AuthService.withdraw), 레이스 컨디션(탈퇴 처리 중 스케줄러가
    // 동시 실행되는 경우) 방어를 위해 매칭 후보 조회 자체에서도 탈퇴 회원(members.deleted_at)을 제외한다.
    @Query("SELECT a FROM ApplicationEntity a JOIN MemberEntity m ON m.id = a.memberId "
            + "WHERE a.gameId = :gameId AND a.status = :status AND m.deletedAt IS NULL "
            + "ORDER BY a.appliedAt ASC")
    List<ApplicationEntity> findByGameIdAndStatusOrderByAppliedAtAsc(
            @Param("gameId") Long gameId, @Param("status") String status);
    List<Long> findDistinctGameIdByStatus(String status);
    boolean existsByMemberIdAndGameIdAndStatusNot(Long memberId, Long gameId, String status);
    long countByGameIdAndStatusNot(Long gameId, String status);
    long countByGameIdAndStatus(Long gameId, String status);
    long countByMemberIdAndStatusAndMatchIdIsNotNullAndCancelledAtGreaterThanEqual(
            Long memberId, String status, LocalDateTime cancelledAt);
}
