package com.sportsmate.server.domain.application.matching;

import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import org.springframework.stereotype.Component;

@Component
public class MatchCandidateFactory {

    public MatchCandidate from(Application application, MemberProfile profile) {
        return new MatchCandidate(
                application.getId(),
                application.getMemberId(),
                profile.team(),
                profile.watchStyles(),
                profile.personality(),
                profile.talkStyle(),
                profile.smokingStatus(),
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
}
