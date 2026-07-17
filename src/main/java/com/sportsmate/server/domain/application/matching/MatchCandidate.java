package com.sportsmate.server.domain.application.matching;

import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.DrinkingPref;
import com.sportsmate.server.domain.member.enums.DrinkingStatus;
import com.sportsmate.server.domain.member.enums.FanLevel;
import com.sportsmate.server.domain.member.enums.FanLevelPref;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.MeetPurpose;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SeatZone;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkPref;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.TeamPref;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record MatchCandidate(
        String applicationId,
        Long memberId,
        String team,
        Long favoriteTeamId,
        TeamPref teamPref,
        FanLevel fanLevel,
        List<WatchStyle> watchStyles,
        List<SeatZone> seatZones,
        Personality personality,
        TalkStyle talkStyle,
        SmokingStatus smokingStatus,
        DrinkingStatus drinkingStatus,
        MeetPurpose meetPurpose,
        String gender,
        LocalDate birthdate,
        Double latitude,
        Double longitude,
        int distanceKm,
        GenderPref genderPref,
        AgePref agePref,
        SmokingPref smokingPref,
        DrinkingPref drinkingPref,
        TalkPref talkPref,
        FanLevelPref fanLevelPref,
        int trustScore,
        Set<Long> rejectedMemberIds) {

    public MatchCandidate {
        watchStyles = watchStyles == null ? List.of() : List.copyOf(watchStyles);
        seatZones = seatZones == null ? List.of() : List.copyOf(seatZones);
        rejectedMemberIds = rejectedMemberIds == null ? Set.of() : Set.copyOf(rejectedMemberIds);
    }

    public MatchCandidate(
            String applicationId,
            Long memberId,
            String team,
            List<WatchStyle> watchStyles,
            Personality personality,
            TalkStyle talkStyle,
            SmokingStatus smokingStatus,
            String gender,
            LocalDate birthdate,
            Double latitude,
            Double longitude,
            int distanceKm,
            GenderPref genderPref,
            AgePref agePref,
            SmokingPref smokingPref,
            int trustScore,
            Set<Long> rejectedMemberIds) {
        this(applicationId, memberId, team, null, null, null, watchStyles, List.of(), personality,
                talkStyle, smokingStatus, null, null, gender, birthdate, latitude, longitude,
                distanceKm, genderPref, agePref, smokingPref, null, null, null, trustScore,
                rejectedMemberIds);
    }
}
