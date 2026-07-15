package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_withdrawal_logs")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWithdrawalLogEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "nickname", length = 50, nullable = false)
    private String nickname;

    @Column(name = "reason", length = 30, nullable = false)
    private String reason;

    @Column(name = "reason_detail", length = 200)
    private String reasonDetail;

    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;
}
