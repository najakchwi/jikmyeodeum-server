package com.sportsmate.server.infrastructure.adapter.out.persistence.notification;

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
@Table(name = "notifications")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "body", length = 1000)
    private String body;

    @Column(name = "target_kind", length = 50)
    private String targetKind;

    @Column(name = "read", nullable = false)
    private Boolean read;
}
