package com.sportsmate.server.infrastructure.adapter.out.persistence.notification;

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
@Table(name = "notification_settings")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSettingsEntity extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "match_request", nullable = false)
    private Boolean matchRequest;

    @Column(name = "match_schedule", nullable = false)
    private Boolean matchSchedule;

    @Column(name = "chat", nullable = false)
    private Boolean chat;

    @Column(name = "review", nullable = false)
    private Boolean review;

    @Column(name = "marketing", nullable = false)
    private Boolean marketing;
}
