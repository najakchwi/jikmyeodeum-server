package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

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
public class MemberLeagueProfileId implements Serializable {

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "league_id")
    private Long leagueId;

    public static MemberLeagueProfileId of(Long memberId, Long leagueId) {
        return new MemberLeagueProfileId(memberId, leagueId);
    }
}
