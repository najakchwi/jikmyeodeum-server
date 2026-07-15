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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewTagId implements Serializable {

    @Column(name = "tag", length = 50, nullable = false)
    private String tag;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    public static ReviewTagId of(String tag, Long reviewId) {
        return new ReviewTagId(tag, reviewId);
    }
}
