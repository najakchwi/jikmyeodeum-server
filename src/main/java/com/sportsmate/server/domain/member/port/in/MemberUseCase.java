package com.sportsmate.server.domain.member.port.in;

import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.time.LocalDate;
import java.util.List;

public interface MemberUseCase {
    MemberProfile get(Long memberId);
    MemberProfile updateProfile(Long memberId, String nickname, String bio, LocalDate birthdate,
            Gender gender, String team);
    MemberProfile updateStyle(Long memberId, String team, List<WatchStyle> watchStyles,
            Personality personality, TalkStyle talkStyle, SmokingStatus smokingStatus);
    MemberProfile updatePreference(Long memberId, GenderPref genderPref, AgePref agePref,
            SmokingPref smokingPref, Integer distanceKm);
    LocationVerifyResult verifyLocation(Long memberId, double latitude, double longitude);
    MemberProfile updateAvatar(Long memberId, String avatarUrl);
    TrustScoreResult getTrustScore(Long memberId);

    record LocationVerifyResult(boolean locationVerified, String depth1, String depth2,
            String depth3) {}

    record TrustScoreResult(int trustScore, String grade, int percentile,
            List<GradeRange> gradeRanges, List<Rule> deductionRules, List<Rule> recoveryRules) {
        public record Rule(String reason, int points) {}
        public record GradeRange(String label, int min, int max) {}
    }
}
