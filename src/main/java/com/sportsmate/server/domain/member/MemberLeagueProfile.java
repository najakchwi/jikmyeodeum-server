package com.sportsmate.server.domain.member;

import com.sportsmate.server.domain.member.enums.FanLevel;
import com.sportsmate.server.domain.member.enums.SeatZone;
import com.sportsmate.server.domain.member.enums.TeamPref;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.util.List;

public record MemberLeagueProfile(
        Long leagueId,
        String leagueCode,
        Long favoriteTeamId,
        TeamPref teamPref,
        FanLevel fanLevel,
        List<WatchStyle> watchStyles,
        List<SeatZone> seatZones) {

    public MemberLeagueProfile {
        watchStyles = watchStyles == null ? List.of() : List.copyOf(watchStyles);
        seatZones = seatZones == null ? List.of() : List.copyOf(seatZones);
    }
}
