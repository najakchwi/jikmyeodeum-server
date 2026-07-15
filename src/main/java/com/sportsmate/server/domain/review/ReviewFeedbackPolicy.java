package com.sportsmate.server.domain.review;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.domain.review.exception.ReviewErrorCode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ReviewFeedbackPolicy {
    public static final Set<String> POSITIVE_TAGS = Set.of(
            "시간 약속을 잘 지켜요",
            "응원 분위기가 좋아요",
            "대화가 잘 통해요",
            "배려심이 넘쳐요",
            "또 만나고 싶어요",
            "편안한 분위기예요");

    public static final Set<String> NEGATIVE_TAGS = Set.of(
            "시간 약속을 안 지켰어요",
            "응원 매너가 아쉬웠어요",
            "대화가 잘 안 통했어요",
            "배려가 부족했어요",
            "다시 만나고 싶지 않아요",
            "분위기가 불편했어요");

    public static final Set<String> PROFILE_MISMATCH_FIELDS = Set.of(
            "team",
            "smoking",
            "watch_style",
            "personality",
            "talk_style");

    private ReviewFeedbackPolicy() {
    }

    public static List<String> validateTags(int rating, List<String> tags) {
        validateRating(rating);
        List<String> normalizedTags = distinctNonBlank(tags);
        Set<String> allowedTags = allowedTags(rating);
        if (allowedTags.isEmpty() && !normalizedTags.isEmpty()) {
            throw new BusinessException(ReviewErrorCode.INVALID_REVIEW_TAG);
        }
        boolean hasInvalidTag = normalizedTags.stream()
                .anyMatch(tag -> !allowedTags.contains(tag));
        if (hasInvalidTag) {
            throw new BusinessException(ReviewErrorCode.INVALID_REVIEW_TAG);
        }
        return normalizedTags;
    }

    public static List<String> validateProfileMismatchFields(
            Boolean profileAccurate, List<String> profileMismatchFields) {
        if (!Boolean.FALSE.equals(profileAccurate)) {
            return List.of();
        }
        List<String> normalizedFields = distinctNonBlank(profileMismatchFields);
        boolean hasInvalidField = normalizedFields.stream()
                .anyMatch(field -> !PROFILE_MISMATCH_FIELDS.contains(field));
        if (hasInvalidField) {
            throw new BusinessException(ReviewErrorCode.INVALID_PROFILE_MISMATCH_FIELD);
        }
        return normalizedFields;
    }

    public static int trustScoreDeltaForRating(int rating) {
        validateRating(rating);
        return switch (rating) {
            case 5 -> 5;
            case 4 -> 2;
            case 3 -> 0;
            case 2 -> -3;
            case 1 -> -5;
            default -> throw new IllegalStateException("Unexpected rating: " + rating);
        };
    }

    private static void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException(ReviewErrorCode.INVALID_REVIEW_TAG);
        }
    }

    private static Set<String> allowedTags(int rating) {
        if (rating >= 4) {
            return POSITIVE_TAGS;
        }
        if (rating <= 2) {
            return NEGATIVE_TAGS;
        }
        return Set.of();
    }

    private static List<String> distinctNonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
