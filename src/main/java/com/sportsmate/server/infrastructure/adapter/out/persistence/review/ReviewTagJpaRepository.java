package com.sportsmate.server.infrastructure.adapter.out.persistence.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewTagJpaRepository extends JpaRepository<ReviewTagEntity, ReviewTagId> {
    List<ReviewTagEntity> findByIdReviewId(Long reviewId);
    List<ReviewTagEntity> findByIdReviewIdInAndIdTagIn(List<Long> reviewIds, List<String> tags);

    @Query("""
            select t.id.tag as tag, count(t) as count
            from ReviewTagEntity t
            join ReviewEntity r on r.id = t.id.reviewId
            where r.targetMemberId = :targetMemberId
              and t.id.tag in :tags
            group by t.id.tag
            order by count(t) desc, t.id.tag asc
            """)
    List<TagCountRow> tagCounts(@Param("targetMemberId") Long targetMemberId, @Param("tags") List<String> tags);

    interface TagCountRow {
        String getTag();
        Long getCount();
    }
}
