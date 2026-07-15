package com.sportsmate.server.infrastructure.adapter.out.persistence.match;

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
@Table(name = "matches")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
