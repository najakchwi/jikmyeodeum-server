package com.sportsmate.server.domain.application.matching;

import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MatchCandidateFactory {

    public MatchCandidate from(Application application, MemberProfile profile) {
        return new MatchCandidate(
                application.getId(),
                application.getMemberId(),
                profile.team(),
                activeWatchStyles(profile.watchStyles()),
                activePersonality(profile.personality()),
                activeTalkStyle(profile.talkStyle()),
                activeSmokingStatus(profile.smokingStatus()),
                profile.gender(),
                profile.birthdate(),
                profile.locationLatitude(),
                profile.locationLongitude(),
                profile.distanceKm(),
                profile.genderPref(),
                profile.agePref(),
                profile.smokingPref(),
                profile.trustScore(),
                application.getRejectedMemberIds());
    }

    private List<WatchStyle> activeWatchStyles(List<WatchStyle> watchStyles) {
        if (watchStyles == null) {
            return List.of();
        }
        return watchStyles.stream()
                .filter(WatchStyle::active)
                .toList();
    }

    private Personality activePersonality(Personality personality) {
        return personality != null && personality.active() ? personality : null;
    }

    private TalkStyle activeTalkStyle(TalkStyle talkStyle) {
        return talkStyle != null && talkStyle.active() ? talkStyle : null;
    }

    private SmokingStatus activeSmokingStatus(SmokingStatus smokingStatus) {
        return smokingStatus != null && smokingStatus.active() ? smokingStatus : null;
    }
}
