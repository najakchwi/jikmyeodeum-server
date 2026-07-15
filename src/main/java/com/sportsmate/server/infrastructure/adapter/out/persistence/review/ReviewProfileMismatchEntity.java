package com.sportsmate.server.infrastructure.adapter.out.persistence.review;

import com.sportsmate.server.common.persistence.BaseTimeEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "review_profile_mismatches")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewProfileMismatchEntity extends BaseTimeEntity {
    @EmbeddedId
    private ReviewProfileMismatchId id;
}
