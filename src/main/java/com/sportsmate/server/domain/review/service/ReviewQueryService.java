package com.sportsmate.server.domain.review.service;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.exception.MemberErrorCode;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.review.port.in.ReviewQueryUseCase;
import com.sportsmate.server.domain.review.port.out.ReviewQueryOutPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewQueryService implements ReviewQueryUseCase {
    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewQueryOutPort reviewQueryOutPort;
    private final MemberOutPort memberOutPort;

    public ReviewQueryService(ReviewQueryOutPort reviewQueryOutPort, MemberOutPort memberOutPort) {
        this.reviewQueryOutPort = reviewQueryOutPort;
        this.memberOutPort = memberOutPort;
    }

    @Override
    public ReceivedReviewsResult getReceivedReviews(Long memberId, String cursor, int size) {
        Member member = memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        ReviewQueryOutPort.ReviewCursor reviewCursor = parseCursor(cursor);

        long count = reviewQueryOutPort.countByTargetMemberId(memberId);
        Map<Integer, Long> distribution = distribution(reviewQueryOutPort.ratingDistribution(memberId));
        List<TagCount> tags = reviewQueryOutPort.tagCounts(memberId).stream()
                .map(tag -> new TagCount(tag.tag(), tag.count()))
                .toList();
        List<ReviewQueryOutPort.ReceivedReviewComment> comments =
                reviewQueryOutPort.comments(memberId, reviewCursor, pageSize + 1);

        boolean hasNext = comments.size() > pageSize;
        List<ReviewComment> items = comments.stream()
                .limit(pageSize)
                .map(comment -> new ReviewComment(
                        formatId(comment.id()), comment.rating(), comment.tags(),
                        comment.comment(), comment.createdAt()))
                .toList();
        String nextCursor = hasNext ? items.get(items.size() - 1).id() : null;

        return new ReceivedReviewsResult(
                new ReviewSummary(roundToOneDecimal(member.getRating()), count),
                distribution,
                tags,
                new CommentPage(items, nextCursor, hasNext));
    }

    private ReviewQueryOutPort.ReviewCursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        return reviewQueryOutPort.findCursor(cursor)
                .orElse(null);
    }

    private Map<Integer, Long> distribution(Map<Integer, Long> counts) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) {
            result.put(rating, counts.getOrDefault(rating, 0L));
        }
        return result;
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String formatId(Long id) {
        return "rv_" + id;
    }
}
