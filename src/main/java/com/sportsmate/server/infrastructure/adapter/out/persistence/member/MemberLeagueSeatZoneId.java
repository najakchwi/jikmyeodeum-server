package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.domain.member.enums.SeatZone;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class MemberLeagueSeatZoneId implements Serializable {

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "league_id")
    private Long leagueId;

    @Column(name = "seat_zone", length = 50)
    @Enumerated(EnumType.STRING)
    private SeatZone seatZone;

    public static MemberLeagueSeatZoneId of(Long memberId, Long leagueId, SeatZone seatZone) {
        return new MemberLeagueSeatZoneId(memberId, leagueId, seatZone);
    }
}
