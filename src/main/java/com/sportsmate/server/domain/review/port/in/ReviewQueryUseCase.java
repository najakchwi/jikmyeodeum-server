package com.sportsmate.server.domain.review.port.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReviewQueryUseCase {
    ReceivedReviewsResult getReceivedReviews(Long memberId, String cursor, int size);

    record ReceivedReviewsResult(
            ReviewSummary summary,
            Map<Integer, Long> distribution,
            List<TagCount> tags,
            CommentPage comments) {}

    record ReviewSummary(double average, long count) {}

    record TagCount(String tag, long count) {}

    record CommentPage(List<ReviewComment> items, String nextCursor, boolean hasNext) {}

    record ReviewComment(String id, int rating, List<String> tags, String comment,
            LocalDateTime createdAt) {}
}
