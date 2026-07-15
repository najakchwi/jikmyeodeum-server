package com.sportsmate.server.infrastructure.adapter.out.persistence.review;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewProfileMismatchId implements Serializable {
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "field", length = 30)
    private String field;

    public static ReviewProfileMismatchId of(Long reviewId, String field) {
        return new ReviewProfileMismatchId(reviewId, field);
    }
}
