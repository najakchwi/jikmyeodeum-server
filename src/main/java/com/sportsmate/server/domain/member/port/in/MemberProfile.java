package com.sportsmate.server.domain.member.port.in;

import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.AgePref;
import java.time.LocalDate;
import java.util.List;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;

public record MemberProfile(
        Long id, String phone, String loginType, String nickname, String bio, LocalDate birthdate,
        String gender, String avatarUrl, String avatarColor, String team, int matchCount,
        double rating, int trustScore, List<WatchStyle> watchStyles, Personality personality,
        TalkStyle talkStyle, SmokingStatus smokingStatus, GenderPref genderPref, AgePref agePref,
        SmokingPref smokingPref, int distanceKm, boolean locationVerified, String locationAddress,
        Double locationLatitude, Double locationLongitude,
        int couponCount, int priorityPassCount, Integer matchScore, boolean phoneVerified) {

    private static final GenderPref DEFAULT_GENDER_PREFERENCE = GenderPref.ANY;
    private static final AgePref DEFAULT_AGE_PREFERENCE = AgePref.ANY;
    private static final SmokingPref DEFAULT_SMOKING_PREFERENCE = SmokingPref.ANY;
    private static final int DEFAULT_DISTANCE_KM = 5;

    public MemberProfile(
            Long id, String phone, String loginType, String nickname, String bio, LocalDate birthdate,
            String gender, String avatarUrl, String avatarColor, String team, int matchCount,
            double rating, int trustScore, List<WatchStyle> watchStyles, Personality personality,
            TalkStyle talkStyle, SmokingStatus smokingStatus, GenderPref genderPref, AgePref agePref,
            SmokingPref smokingPref, int distanceKm, boolean locationVerified, String locationAddress,
            Double locationLatitude, Double locationLongitude,
            int couponCount, int priorityPassCount, Integer matchScore) {
        this(id, phone, loginType, nickname, bio, birthdate, gender, avatarUrl, avatarColor, team,
                matchCount, rating, trustScore, watchStyles, personality, talkStyle, smokingStatus,
                genderPref, agePref, smokingPref, distanceKm, locationVerified, locationAddress,
                locationLatitude, locationLongitude, couponCount, priorityPassCount, matchScore,
                phone != null && !phone.isBlank());
    }

    public static MemberProfile from(Member member) {
        return new MemberProfile(
                member.getId(), member.getPhone(), member.getLoginType().name().toLowerCase(),
                member.getNickname(), member.getBio(), member.getBirthdate(),
                member.getGender().name().toLowerCase(), member.getAvatarUrl(),
                member.getAvatarColor(), member.getTeam(), member.getMatchCount(),
                member.getRating(), member.getTrustScore(), member.getWatchStyles(),
                member.getPersonality(), member.getTalkStyle(), member.getSmokingStatus(),
                genderPreferenceOrDefault(member.getGenderPref()), agePreferenceOrDefault(member.getAgePref()),
                smokingPreferenceOrDefault(member.getSmokingPref()), distanceOrDefault(member.getDistanceKm()),
                member.isLocationVerified(), member.getLocationAddress(),
                member.getLatitude(), member.getLongitude(),
                member.getCouponCount(), member.getPriorityPassCount(), null, member.isPhoneVerified());
    }

    public MemberProfile withMatchScore(Integer matchScore) {
        return new MemberProfile(
                id, phone, loginType, nickname, bio, birthdate, gender, avatarUrl, avatarColor, team,
                matchCount, rating, trustScore, watchStyles, personality, talkStyle, smokingStatus,
                genderPref, agePref, smokingPref, distanceKm, locationVerified, locationAddress,
                locationLatitude, locationLongitude, couponCount, priorityPassCount, matchScore, phoneVerified);
    }

    private static GenderPref genderPreferenceOrDefault(GenderPref preference) {
        return preference == null ? DEFAULT_GENDER_PREFERENCE : preference;
    }

    private static AgePref agePreferenceOrDefault(AgePref preference) {
        return preference == null ? DEFAULT_AGE_PREFERENCE : preference;
    }

    private static SmokingPref smokingPreferenceOrDefault(SmokingPref preference) {
        return preference == null ? DEFAULT_SMOKING_PREFERENCE : preference;
    }

    private static int distanceOrDefault(int distanceKm) {
        return distanceKm == 0 ? DEFAULT_DISTANCE_KM : distanceKm;
    }
}
