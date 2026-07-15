package com.sportsmate.server.infrastructure.adapter.out.persistence.review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {
    boolean existsByMatchIdAndReviewerId(Long matchId, Long reviewerId);
    Optional<ReviewEntity> findByMatchIdAndReviewerId(Long matchId, Long reviewerId);
    long countByTargetMemberId(Long targetMemberId);

    @Query("""
            select r.rating as rating, count(r) as count
            from ReviewEntity r
            where r.targetMemberId = :targetMemberId
            group by r.rating
            """)
    List<RatingCountRow> ratingDistribution(@Param("targetMemberId") Long targetMemberId);

    @Query("""
            select r
            from ReviewEntity r
            where r.targetMemberId = :targetMemberId
              and r.comment is not null
              and trim(r.comment) <> ''
            order by r.createdAt desc, r.id desc
            """)
    List<ReviewEntity> findComments(@Param("targetMemberId") Long targetMemberId, Pageable pageable);

    @Query("""
            select r
            from ReviewEntity r
            where r.targetMemberId = :targetMemberId
              and r.comment is not null
              and trim(r.comment) <> ''
              and (r.createdAt < :cursorCreatedAt
                   or (r.createdAt = :cursorCreatedAt and r.id < :cursorId))
            order by r.createdAt desc, r.id desc
            """)
    List<ReviewEntity> findCommentsAfterCursor(
            @Param("targetMemberId") Long targetMemberId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    interface RatingCountRow {
        Integer getRating();
        Long getCount();
    }
}
