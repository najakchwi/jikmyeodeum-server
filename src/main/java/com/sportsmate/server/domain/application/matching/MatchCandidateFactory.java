package com.sportsmate.server.domain.application.matching;

import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.member.MemberLeagueProfile;
import com.sportsmate.server.domain.member.enums.FanLevel;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SeatZone;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MatchCandidateFactory {

    public MatchCandidate from(Application application, MemberProfile profile, Long leagueId) {
        MemberLeagueProfile leagueProfile = profile.leagueProfiles().stream()
                .filter(item -> item.leagueId().equals(leagueId))
                .findFirst()
                .orElseGet(() -> fallbackLeagueProfile(profile, leagueId));
        return new MatchCandidate(
                application.getId(),
                application.getMemberId(),
                profile.team(),
                leagueProfile.favoriteTeamId(),
                leagueProfile.teamPref(),
                activeFanLevel(leagueProfile.fanLevel()),
                activeWatchStyles(leagueProfile.watchStyles()),
                activeSeatZones(leagueProfile.seatZones()),
                activePersonality(profile.personality()),
                activeTalkStyle(profile.talkStyle()),
                activeSmokingStatus(profile.smokingStatus()),
                profile.drinkingStatus(),
                profile.meetPurpose(),
                profile.gender(),
                profile.birthdate(),
                profile.locationLatitude(),
                profile.locationLongitude(),
                profile.distanceKm(),
                profile.genderPref(),
                profile.agePref(),
                profile.smokingPref(),
                profile.drinkingPref(),
                profile.talkPref(),
                profile.fanLevelPref(),
                profile.trustScore(),
                application.getRejectedMemberIds());
    }

    public MatchCandidate from(Application application, MemberProfile profile) {
        Long leagueId = profile.leagueProfiles().stream()
                .findFirst()
                .map(MemberLeagueProfile::leagueId)
                .orElse(1L);
        return from(application, profile, leagueId);
    }

    private MemberLeagueProfile fallbackLeagueProfile(MemberProfile profile, Long leagueId) {
        if (profile.leagueProfiles().isEmpty()) {
            return new MemberLeagueProfile(leagueId, null, null, null, null,
                    profile.watchStyles(), List.of());
        }
        throw new IllegalArgumentException("League profile is required: " + leagueId);
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

    private FanLevel activeFanLevel(FanLevel fanLevel) {
        return fanLevel != null && fanLevel.active() ? fanLevel : null;
    }

    private List<SeatZone> activeSeatZones(List<SeatZone> seatZones) {
        if (seatZones == null) {
            return List.of();
        }
        return seatZones.stream()
                .filter(SeatZone::active)
                .toList();
    }

    private TalkStyle activeTalkStyle(TalkStyle talkStyle) {
        return talkStyle != null && talkStyle.active() ? talkStyle : null;
    }

    private SmokingStatus activeSmokingStatus(SmokingStatus smokingStatus) {
        return smokingStatus != null && smokingStatus.active() ? smokingStatus : null;
    }
}
