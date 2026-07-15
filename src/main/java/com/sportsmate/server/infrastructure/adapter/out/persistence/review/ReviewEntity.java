package com.sportsmate.server.infrastructure.adapter.out.persistence.review;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "target_member_id", nullable = false)
    private Long targetMemberId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "profile_accurate")
    private Boolean profileAccurate;
}
