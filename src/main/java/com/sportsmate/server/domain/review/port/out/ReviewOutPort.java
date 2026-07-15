package com.sportsmate.server.domain.review.port.out;

import com.sportsmate.server.domain.review.port.dto.ReviewDetail;
import java.util.List;
import java.util.Optional;

public interface ReviewOutPort {
    boolean existsByMatchIdAndReviewerId(String matchId, Long reviewerId);
    Optional<ReviewDetail> findByMatchIdAndReviewerId(String matchId, Long reviewerId);
    void save(String matchId, Long reviewerId, Long targetMemberId, int rating,
            List<String> tags, String comment, Boolean profileAccurate,
            List<String> profileMismatchFields);
}
