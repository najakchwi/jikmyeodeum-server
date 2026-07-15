package com.sportsmate.server.infrastructure.adapter.out.persistence.review;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.review.ReviewFeedbackPolicy;
import com.sportsmate.server.domain.review.port.dto.ReviewDetail;
import com.sportsmate.server.domain.review.port.out.ReviewQueryOutPort;
import com.sportsmate.server.domain.review.port.out.ReviewOutPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

@PersistenceAdapter
public class ReviewPersistenceAdapter implements ReviewOutPort, ReviewQueryOutPort {
    private final ReviewJpaRepository repository;
    private final ReviewTagJpaRepository tagRepository;
    private final ReviewProfileMismatchJpaRepository profileMismatchRepository;

    public ReviewPersistenceAdapter(ReviewJpaRepository repository, ReviewTagJpaRepository tagRepository,
            ReviewProfileMismatchJpaRepository profileMismatchRepository) {
        this.repository = repository;
        this.tagRepository = tagRepository;
        this.profileMismatchRepository = profileMismatchRepository;
    }
    @Override public boolean existsByMatchIdAndReviewerId(String matchId, Long reviewerId) {
        return repository.existsByMatchIdAndReviewerId(Long.parseLong(matchId), reviewerId);
    }
    @Override public Optional<ReviewDetail> findByMatchIdAndReviewerId(String matchId, Long reviewerId) {
        return repository.findByMatchIdAndReviewerId(Long.parseLong(matchId), reviewerId)
                .map(this::toReviewDetail);
    }

    @Override public void save(String matchId, Long reviewerId, Long targetMemberId,
            int rating, List<String> tags, String comment, Boolean profileAccurate,
            List<String> profileMismatchFields) {
        ReviewEntity review = repository.save(ReviewEntity.builder()
                .matchId(Long.parseLong(matchId))
                .reviewerId(reviewerId)
                .targetMemberId(targetMemberId)
                .rating(rating)
                .comment(comment)
                .profileAccurate(profileAccurate)
                .build());
        tagRepository.saveAll(tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .distinct()
                .map(tag -> ReviewTagEntity.builder()
                        .id(ReviewTagId.of(tag, review.getId()))
                        .build())
                .toList());
        profileMismatchRepository.saveAll(profileMismatchFields.stream()
                .filter(field -> field != null && !field.isBlank())
                .distinct()
                .map(field -> ReviewProfileMismatchEntity.builder()
                        .id(ReviewProfileMismatchId.of(review.getId(), field))
                        .build())
                .toList());
    }

    @Override
    public long countByTargetMemberId(Long targetMemberId) {
        return repository.countByTargetMemberId(targetMemberId);
    }

    @Override
    public Map<Integer, Long> ratingDistribution(Long targetMemberId) {
        return repository.ratingDistribution(targetMemberId).stream()
                .collect(Collectors.toMap(
                        ReviewJpaRepository.RatingCountRow::getRating,
                        ReviewJpaRepository.RatingCountRow::getCount));
    }

    @Override
    public List<TagCount> tagCounts(Long targetMemberId) {
        return tagRepository.tagCounts(targetMemberId, positiveTags()).stream()
                .map(row -> new TagCount(row.getTag(), row.getCount()))
                .toList();
    }

    @Override
    public Optional<ReviewCursor> findCursor(String cursor) {
        return parseCursorId(cursor)
                .flatMap(repository::findById)
                .map(entity -> new ReviewCursor(entity.getId(), entity.getCreatedAt()));
    }

    @Override
    public List<ReceivedReviewComment> comments(Long targetMemberId, ReviewCursor cursor, int size) {
        PageRequest page = PageRequest.of(0, size);
        List<ReviewEntity> comments = cursor == null
                ? repository.findComments(targetMemberId, page)
                : repository.findCommentsAfterCursor(
                        targetMemberId, cursor.createdAt(), cursor.id(), page);
        Map<Long, List<String>> tagsByReviewId = tagsByReviewId(comments);
        return comments.stream()
                .map(entity -> new ReceivedReviewComment(
                        entity.getId(),
                        entity.getRating(),
                        tagsByReviewId.getOrDefault(entity.getId(), List.of()),
                        entity.getComment(),
                        entity.getCreatedAt()))
                .toList();
    }

    private Optional<Long> parseCursorId(String cursor) {
        String value = cursor.startsWith("rv_") ? cursor.substring(3) : cursor;
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Map<Long, List<String>> tagsByReviewId(List<ReviewEntity> comments) {
        List<Long> reviewIds = comments.stream()
                .map(ReviewEntity::getId)
                .toList();
        if (reviewIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> grouped = tagRepository.findByIdReviewIdInAndIdTagIn(reviewIds, positiveTags()).stream()
                .collect(Collectors.groupingBy(
                        tag -> tag.getId().getReviewId(),
                        LinkedHashMap::new,
                        Collectors.mapping(tag -> tag.getId().getTag(), Collectors.toList())));
        return grouped;
    }

    private ReviewDetail toReviewDetail(ReviewEntity entity) {
        return new ReviewDetail(
                entity.getRating(),
                tagRepository.findByIdReviewId(entity.getId()).stream()
                        .map(tag -> tag.getId().getTag())
                        .toList(),
                entity.getComment(),
                entity.getProfileAccurate(),
                profileMismatchRepository.findByIdReviewId(entity.getId()).stream()
                        .map(mismatch -> mismatch.getId().getField())
                        .toList(),
                entity.getCreatedAt());
    }

    private List<String> positiveTags() {
        return ReviewFeedbackPolicy.POSITIVE_TAGS.stream().toList();
    }
}
