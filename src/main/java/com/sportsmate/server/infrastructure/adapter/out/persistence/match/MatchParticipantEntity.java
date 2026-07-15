package com.sportsmate.server.infrastructure.adapter.out.persistence.match;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_participants")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchParticipantEntity extends BaseTimeEntity {

    @EmbeddedId
    private MatchParticipantId id;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "response", length = 20, nullable = false)
    private String response;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
}
