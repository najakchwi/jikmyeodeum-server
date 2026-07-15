package com.sportsmate.server.domain.application.matching;

import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record MatchCandidate(
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

    public MatchCandidate {
        watchStyles = watchStyles == null ? List.of() : List.copyOf(watchStyles);
        rejectedMemberIds = rejectedMemberIds == null ? Set.of() : Set.copyOf(rejectedMemberIds);
    }
}
