package com.sportsmate.server.infrastructure.adapter.out.persistence.match;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchParticipantId implements Serializable {

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public static MatchParticipantId of(Long matchId, Long memberId) {
        return new MatchParticipantId(matchId, memberId);
    }
}
