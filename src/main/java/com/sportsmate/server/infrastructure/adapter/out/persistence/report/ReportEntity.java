package com.sportsmate.server.infrastructure.adapter.out.persistence.report;

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
@Table(name = "reports")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "target_member_id", nullable = false)
    private Long targetMemberId;

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "reason", length = 50, nullable = false)
    private String reason;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "status", length = 20, nullable = false)
    private String status;
}
