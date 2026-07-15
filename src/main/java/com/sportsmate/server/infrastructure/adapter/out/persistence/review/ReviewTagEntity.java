package com.sportsmate.server.infrastructure.adapter.out.persistence.review;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_tags")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewTagEntity extends BaseTimeEntity {

    @EmbeddedId
    private ReviewTagId id;
}
