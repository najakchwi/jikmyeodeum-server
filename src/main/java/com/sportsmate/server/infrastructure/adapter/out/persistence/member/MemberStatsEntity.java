package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_stats")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStatsEntity extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "match_count", nullable = false)
    private Integer matchCount;

    @Column(name = "rating", nullable = false)
    private Double rating;

    @Column(name = "trust_score", nullable = false)
    private Integer trustScore;

    @Column(name = "coupon_count", nullable = false)
    private Integer couponCount;

    @Column(name = "priority_pass_count", nullable = false)
    private Integer priorityPassCount;
}
