package com.sportsmate.server.infrastructure.adapter.out.persistence.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewProfileMismatchJpaRepository
        extends JpaRepository<ReviewProfileMismatchEntity, ReviewProfileMismatchId> {
    List<ReviewProfileMismatchEntity> findByIdReviewId(Long reviewId);
}
