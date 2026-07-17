package com.sportsmate.server.domain.member.port.in;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.MemberLeagueProfile;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.DrinkingPref;
import com.sportsmate.server.domain.member.enums.DrinkingStatus;
import com.sportsmate.server.domain.member.enums.FanLevelPref;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.MeetPurpose;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkPref;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record MemberProfile(
        Long id, String phone, String loginType, String nickname, String bio, LocalDate birthdate,
        String gender, String avatarUrl, String avatarColor, String team, int matchCount,
        double rating, int trustScore, List<WatchStyle> watchStyles, Personality personality,
        TalkStyle talkStyle, SmokingStatus smokingStatus, DrinkingStatus drinkingStatus,
        MeetPurpose meetPurpose, GenderPref genderPref, AgePref agePref,
        SmokingPref smokingPref, DrinkingPref drinkingPref, TalkPref talkPref,
        FanLevelPref fanLevelPref, int distanceKm, boolean locationVerified, String locationAddress,
        Double locationLatitude, Double locationLongitude,
        int couponCount, int priorityPassCount, Integer matchScore, boolean phoneVerified,
        List<MemberLeagueProfile> leagueProfiles,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<MatchReasonSummary> matchReasons) {

    private static final GenderPref DEFAULT_GENDER_PREFERENCE = GenderPref.ANY;
    private static final AgePref DEFAULT_AGE_PREFERENCE = AgePref.ANY;
    private static final SmokingPref DEFAULT_SMOKING_PREFERENCE = SmokingPref.ANY;
    private static final DrinkingPref DEFAULT_DRINKING_PREFERENCE = DrinkingPref.ANY;
    private static final TalkPref DEFAULT_TALK_PREFERENCE = TalkPref.ANY;
    private static final FanLevelPref DEFAULT_FAN_LEVEL_PREFERENCE = FanLevelPref.ANY;
    private static final int DEFAULT_DISTANCE_KM = 5;

    public MemberProfile {
        watchStyles = watchStyles == null ? List.of() : List.copyOf(watchStyles);
        leagueProfiles = leagueProfiles == null ? List.of() : List.copyOf(leagueProfiles);
        matchReasons = matchReasons == null ? null : List.copyOf(matchReasons);
    }

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
                null, null, genderPref, agePref, smokingPref, null, null, null,
                distanceKm, locationVerified, locationAddress,
                locationLatitude, locationLongitude, couponCount, priorityPassCount, matchScore,
                phone != null && !phone.isBlank(), List.of(), null);
    }

    public static MemberProfile from(Member member) {
        return new MemberProfile(
                member.getId(), member.getPhone(), member.getLoginType().name().toLowerCase(),
                member.getNickname(), member.getBio(), member.getBirthdate(),
                member.getGender().name().toLowerCase(), member.getAvatarUrl(),
                member.getAvatarColor(), member.getTeam(), member.getMatchCount(),
                member.getRating(), member.getTrustScore(), member.getWatchStyles(),
                member.getPersonality(), member.getTalkStyle(), member.getSmokingStatus(),
                member.getDrinkingStatus(), member.getMeetPurpose(),
                genderPreferenceOrDefault(member.getGenderPref()), agePreferenceOrDefault(member.getAgePref()),
                smokingPreferenceOrDefault(member.getSmokingPref()),
                drinkingPreferenceOrDefault(member.getDrinkingPref()),
                talkPreferenceOrDefault(member.getTalkPref()),
                fanLevelPreferenceOrDefault(member.getFanLevelPref()),
                distanceOrDefault(member.getDistanceKm()),
                member.isLocationVerified(), member.getLocationAddress(),
                member.getLatitude(), member.getLongitude(),
                member.getCouponCount(), member.getPriorityPassCount(), null, member.isPhoneVerified(),
                member.getLeagueProfiles(), null);
    }

    public MemberProfile withMatchScore(Integer matchScore) {
        return new MemberProfile(
                id, phone, loginType, nickname, bio, birthdate, gender, avatarUrl, avatarColor, team,
                matchCount, rating, trustScore, watchStyles, personality, talkStyle, smokingStatus,
                drinkingStatus, meetPurpose, genderPref, agePref, smokingPref, drinkingPref,
                talkPref, fanLevelPref, distanceKm, locationVerified, locationAddress,
                locationLatitude, locationLongitude, couponCount, priorityPassCount, matchScore,
                phoneVerified, leagueProfiles, matchReasons);
    }

    public MemberProfile withMatchReasons(List<MatchReasonSummary> matchReasons) {
        return new MemberProfile(
                id, phone, loginType, nickname, bio, birthdate, gender, avatarUrl, avatarColor, team,
                matchCount, rating, trustScore, watchStyles, personality, talkStyle, smokingStatus,
                drinkingStatus, meetPurpose, genderPref, agePref, smokingPref, drinkingPref,
                talkPref, fanLevelPref, distanceKm, locationVerified, locationAddress,
                locationLatitude, locationLongitude, couponCount, priorityPassCount, matchScore,
                phoneVerified, leagueProfiles, matchReasons == null ? List.of() : matchReasons);
    }

    public MemberProfile withLeagueProfile(MemberLeagueProfile leagueProfile) {
        ArrayList<MemberLeagueProfile> profiles = new ArrayList<>(leagueProfiles);
        profiles.removeIf(existing -> existing.leagueId().equals(leagueProfile.leagueId()));
        profiles.add(leagueProfile);
        return new MemberProfile(
                id, phone, loginType, nickname, bio, birthdate, gender, avatarUrl, avatarColor, team,
                matchCount, rating, trustScore, watchStyles, personality, talkStyle, smokingStatus,
                drinkingStatus, meetPurpose, genderPref, agePref, smokingPref, drinkingPref,
                talkPref, fanLevelPref, distanceKm, locationVerified, locationAddress,
                locationLatitude, locationLongitude, couponCount, priorityPassCount, matchScore,
                phoneVerified, profiles, matchReasons);
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

    private static DrinkingPref drinkingPreferenceOrDefault(DrinkingPref preference) {
        return preference == null ? DEFAULT_DRINKING_PREFERENCE : preference;
    }

    private static TalkPref talkPreferenceOrDefault(TalkPref preference) {
        return preference == null ? DEFAULT_TALK_PREFERENCE : preference;
    }

    private static FanLevelPref fanLevelPreferenceOrDefault(FanLevelPref preference) {
        return preference == null ? DEFAULT_FAN_LEVEL_PREFERENCE : preference;
    }

    private static int distanceOrDefault(int distanceKm) {
        return distanceKm == 0 ? DEFAULT_DISTANCE_KM : distanceKm;
    }

    public record MatchReasonSummary(String key, String label) {
    }
}
