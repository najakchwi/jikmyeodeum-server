package com.sportsmate.server.domain.review.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReviewQueryOutPort {
    long countByTargetMemberId(Long targetMemberId);
    Map<Integer, Long> ratingDistribution(Long targetMemberId);
    List<TagCount> tagCounts(Long targetMemberId);
    Optional<ReviewCursor> findCursor(String cursor);
    List<ReceivedReviewComment> comments(Long targetMemberId, ReviewCursor cursor, int size);

    record TagCount(String tag, long count) {}

    record ReviewCursor(Long id, LocalDateTime createdAt) {}

    record ReceivedReviewComment(Long id, int rating, List<String> tags, String comment,
            LocalDateTime createdAt) {}
}
