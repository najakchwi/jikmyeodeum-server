package com.sportsmate.server.infrastructure.adapter.out.persistence.application;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "match_applications")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "matched_member_id")
    private Long matchedMemberId;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "response", length = 20)
    private String response;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "match_score")
    private Integer matchScore;

    @ElementCollection
    @CollectionTable(
            name = "match_application_rejected_members",
            joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "rejected_member_id", nullable = false)
    @Builder.Default
    private Set<Long> rejectedMemberIds = new LinkedHashSet<>();
}
