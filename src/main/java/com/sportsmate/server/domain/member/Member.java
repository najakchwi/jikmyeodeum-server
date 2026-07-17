package com.sportsmate.server.domain.member;

import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.DrinkingPref;
import com.sportsmate.server.domain.member.enums.DrinkingStatus;
import com.sportsmate.server.domain.member.enums.FanLevelPref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.MeetPurpose;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkPref;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Member {

    private Long id;
    private String phone;
    private LocalDateTime phoneVerifiedAt;
    private String password;
    private LoginType loginType;
    private String providerId;
    private String nickname;
    private String bio;
    private LocalDate birthdate;
    private Gender gender;
    private String avatarUrl;
    private String avatarColor;
    private String team;
    private List<WatchStyle> watchStyles;
    private Personality personality;
    private TalkStyle talkStyle;
    private SmokingStatus smokingStatus;
    private DrinkingStatus drinkingStatus;
    private MeetPurpose meetPurpose;
    private GenderPref genderPref;
    private AgePref agePref;
    private SmokingPref smokingPref;
    private DrinkingPref drinkingPref;
    private TalkPref talkPref;
    private FanLevelPref fanLevelPref;
    private int distanceKm;
    private boolean locationVerified;
    private String locationAddress;
    private Double latitude;
    private Double longitude;
    private int matchCount;
    private double rating;
    private int trustScore;
    private int couponCount;
    private int priorityPassCount;
    private boolean marketingAgreed;
    private Role role;
    private List<MemberLeagueProfile> leagueProfiles;

    public static Member create(
            String phone,
            String password,
            LoginType loginType,
            String providerId,
            String nickname,
            String bio,
            LocalDate birthdate,
            Gender gender,
            String team,
            List<WatchStyle> watchStyles,
            Personality personality,
            TalkStyle talkStyle,
            SmokingStatus smokingStatus,
            GenderPref genderPref,
            AgePref agePref,
            SmokingPref smokingPref,
            int distanceKm,
            String locationAddress,
            Double latitude,
            Double longitude,
            boolean marketingAgreed) {
        Member member = new Member();
        member.phone = phone;
        member.phoneVerifiedAt = phone == null || phone.isBlank() ? null : LocalDateTime.now();
        member.password = password;
        member.loginType = loginType;
        member.providerId = providerId;
        member.nickname = nickname;
        member.bio = bio == null ? "" : bio;
        member.birthdate = birthdate;
        member.gender = gender;
        member.avatarColor = "#2E7D32";
        member.team = team;
        member.watchStyles = List.copyOf(watchStyles);
        member.personality = personality;
        member.talkStyle = talkStyle;
        member.smokingStatus = smokingStatus;
        member.genderPref = genderPref;
        member.agePref = agePref;
        member.smokingPref = smokingPref;
        member.drinkingPref = DrinkingPref.ANY;
        member.talkPref = TalkPref.ANY;
        member.fanLevelPref = FanLevelPref.ANY;
        member.distanceKm = distanceKm;
        member.locationVerified = latitude != null && longitude != null;
        member.locationAddress = locationAddress == null ? "" : locationAddress;
        member.latitude = latitude;
        member.longitude = longitude;
        member.trustScore = 100;
        member.rating = 0.0;
        member.couponCount = 2;
        member.marketingAgreed = marketingAgreed;
        member.role = Role.USER;
        member.leagueProfiles = List.of();
        return member;
    }

    public static Member reconstitute(
            Long id, String phone, LocalDateTime phoneVerifiedAt, String password, LoginType loginType, String providerId,
            String nickname, String bio, LocalDate birthdate, Gender gender, String avatarUrl,
            String avatarColor, String team, List<WatchStyle> watchStyles, Personality personality,
            TalkStyle talkStyle, SmokingStatus smokingStatus, GenderPref genderPref, AgePref agePref,
            SmokingPref smokingPref, int distanceKm, boolean locationVerified, String locationAddress,
            Double latitude, Double longitude, int matchCount, double rating, int trustScore,
            int couponCount, int priorityPassCount, boolean marketingAgreed, Role role) {
        return reconstitute(
                id, phone, phoneVerifiedAt, password, loginType, providerId, nickname, bio,
                birthdate, gender, avatarUrl, avatarColor, team, watchStyles, personality,
                talkStyle, smokingStatus, null, null, genderPref, agePref, smokingPref,
                null, null, null, distanceKm, locationVerified, locationAddress, latitude,
                longitude, matchCount, rating, trustScore, couponCount, priorityPassCount,
                marketingAgreed, role, List.of());
    }

    public static Member reconstitute(
            Long id, String phone, LocalDateTime phoneVerifiedAt, String password, LoginType loginType, String providerId,
            String nickname, String bio, LocalDate birthdate, Gender gender, String avatarUrl,
            String avatarColor, String team, List<WatchStyle> watchStyles, Personality personality,
            TalkStyle talkStyle, SmokingStatus smokingStatus, DrinkingStatus drinkingStatus,
            MeetPurpose meetPurpose, GenderPref genderPref, AgePref agePref, SmokingPref smokingPref,
            DrinkingPref drinkingPref, TalkPref talkPref, FanLevelPref fanLevelPref, int distanceKm,
            boolean locationVerified, String locationAddress, Double latitude, Double longitude,
            int matchCount, double rating, int trustScore, int couponCount, int priorityPassCount,
            boolean marketingAgreed, Role role, List<MemberLeagueProfile> leagueProfiles) {
        Member member = new Member();
        member.id = id;
        member.phone = phone;
        member.phoneVerifiedAt = phoneVerifiedAt;
        member.password = password;
        member.loginType = loginType;
        member.providerId = providerId;
        member.nickname = nickname;
        member.bio = bio;
        member.birthdate = birthdate;
        member.gender = gender;
        member.avatarUrl = avatarUrl;
        member.avatarColor = avatarColor;
        member.team = team;
        member.watchStyles = List.copyOf(watchStyles);
        member.personality = personality;
        member.talkStyle = talkStyle;
        member.smokingStatus = smokingStatus;
        member.drinkingStatus = drinkingStatus;
        member.meetPurpose = meetPurpose;
        member.genderPref = genderPref;
        member.agePref = agePref;
        member.smokingPref = smokingPref;
        member.drinkingPref = drinkingPref;
        member.talkPref = talkPref;
        member.fanLevelPref = fanLevelPref;
        member.distanceKm = distanceKm;
        member.locationVerified = locationVerified;
        member.locationAddress = locationAddress;
        member.latitude = latitude;
        member.longitude = longitude;
        member.matchCount = matchCount;
        member.rating = rating;
        member.trustScore = trustScore;
        member.couponCount = couponCount;
        member.priorityPassCount = priorityPassCount;
        member.marketingAgreed = marketingAgreed;
        member.role = role;
        member.leagueProfiles = leagueProfiles == null ? List.of() : List.copyOf(leagueProfiles);
        return member;
    }

    public static Member reconstitute(
            Long id, String phone, String password, LoginType loginType, String providerId,
            String nickname, String bio, LocalDate birthdate, Gender gender, String avatarUrl,
            String avatarColor, String team, List<WatchStyle> watchStyles, Personality personality,
            TalkStyle talkStyle, SmokingStatus smokingStatus, GenderPref genderPref, AgePref agePref,
            SmokingPref smokingPref, int distanceKm, boolean locationVerified, String locationAddress,
            Double latitude, Double longitude, int matchCount, double rating, int trustScore,
            int couponCount, int priorityPassCount, boolean marketingAgreed, Role role) {
        return reconstitute(
                id, phone, phone == null || phone.isBlank() ? null : LocalDateTime.now(),
                password, loginType, providerId, nickname, bio, birthdate, gender, avatarUrl,
                avatarColor, team, watchStyles, personality, talkStyle, smokingStatus, genderPref,
                agePref, smokingPref, distanceKm, locationVerified, locationAddress, latitude,
                longitude, matchCount, rating, trustScore, couponCount, priorityPassCount,
                marketingAgreed, role);
    }

    public void updateProfile(String nickname, String bio, LocalDate birthdate, Gender gender, String team) {
        if (nickname != null) this.nickname = nickname;
        if (bio != null) this.bio = bio;
        if (birthdate != null) this.birthdate = birthdate;
        if (gender != null) this.gender = gender;
        if (team != null) this.team = team;
    }

    public void updateStyle(String team, List<WatchStyle> watchStyles, Personality personality,
            TalkStyle talkStyle, SmokingStatus smokingStatus) {
        if (team != null) this.team = team;
        if (watchStyles != null) this.watchStyles = List.copyOf(watchStyles);
        if (personality != null) this.personality = personality;
        if (talkStyle != null) this.talkStyle = talkStyle;
        if (smokingStatus != null) this.smokingStatus = smokingStatus;
    }

    public void updateStyle(Personality personality, TalkStyle talkStyle, SmokingStatus smokingStatus,
            DrinkingStatus drinkingStatus, MeetPurpose meetPurpose) {
        if (personality != null) this.personality = personality;
        if (talkStyle != null) this.talkStyle = talkStyle;
        if (smokingStatus != null) this.smokingStatus = smokingStatus;
        if (drinkingStatus != null) this.drinkingStatus = drinkingStatus;
        if (meetPurpose != null) this.meetPurpose = meetPurpose;
    }

    public void updatePreference(GenderPref genderPref, AgePref agePref, SmokingPref smokingPref,
            DrinkingPref drinkingPref, TalkPref talkPref, FanLevelPref fanLevelPref, Integer distanceKm) {
        if (genderPref != null) this.genderPref = genderPref;
        if (agePref != null) this.agePref = agePref;
        if (smokingPref != null) this.smokingPref = smokingPref;
        if (drinkingPref != null) this.drinkingPref = drinkingPref;
        if (talkPref != null) this.talkPref = talkPref;
        if (fanLevelPref != null) this.fanLevelPref = fanLevelPref;
        if (distanceKm != null) this.distanceKm = distanceKm;
    }

    public void updatePreference(GenderPref genderPref, AgePref agePref, SmokingPref smokingPref, Integer distanceKm) {
        updatePreference(genderPref, agePref, smokingPref, null, null, null, distanceKm);
    }

    public void upsertLeagueProfile(MemberLeagueProfile profile) {
        List<MemberLeagueProfile> updated = new java.util.ArrayList<>(leagueProfiles == null ? List.of() : leagueProfiles);
        updated.removeIf(existing -> existing.leagueId().equals(profile.leagueId()));
        updated.add(profile);
        this.leagueProfiles = List.copyOf(updated);
    }

    public void verifyLocation(String address, double latitude, double longitude) {
        this.locationAddress = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationVerified = true;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changePhone(String phone) {
        this.phone = phone;
        this.phoneVerifiedAt = LocalDateTime.now();
    }

    public void changeAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void addTrustScore(int points) {
        this.trustScore = Math.max(0, Math.min(100, this.trustScore + points));
    }

    public void addCoupon() {
        this.couponCount++;
        if (this.couponCount >= 5) {
            this.couponCount -= 5;
            this.priorityPassCount++;
        }
    }

    public void receiveRating(double newRating) {
        this.rating = matchCount == 0 ? newRating : ((this.rating * matchCount) + newRating) / (matchCount + 1);
        this.matchCount++;
    }

    public Long getId() { return id; }
    public String getPhone() { return phone; }
    public LocalDateTime getPhoneVerifiedAt() { return phoneVerifiedAt; }
    public boolean isPhoneVerified() { return phone != null && phoneVerifiedAt != null; }
    public String getPassword() { return password; }
    public LoginType getLoginType() { return loginType; }
    public String getProviderId() { return providerId; }
    public String getNickname() { return nickname; }
    public String getBio() { return bio; }
    public LocalDate getBirthdate() { return birthdate; }
    public Gender getGender() { return gender; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getAvatarColor() { return avatarColor; }
    public String getTeam() { return team; }
    public List<WatchStyle> getWatchStyles() { return watchStyles; }
    public Personality getPersonality() { return personality; }
    public TalkStyle getTalkStyle() { return talkStyle; }
    public SmokingStatus getSmokingStatus() { return smokingStatus; }
    public DrinkingStatus getDrinkingStatus() { return drinkingStatus; }
    public MeetPurpose getMeetPurpose() { return meetPurpose; }
    public GenderPref getGenderPref() { return genderPref; }
    public AgePref getAgePref() { return agePref; }
    public SmokingPref getSmokingPref() { return smokingPref; }
    public DrinkingPref getDrinkingPref() { return drinkingPref; }
    public TalkPref getTalkPref() { return talkPref; }
    public FanLevelPref getFanLevelPref() { return fanLevelPref; }
    public int getDistanceKm() { return distanceKm; }
    public boolean isLocationVerified() { return locationVerified; }
    public String getLocationAddress() { return locationAddress; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public int getMatchCount() { return matchCount; }
    public double getRating() { return rating; }
    public int getTrustScore() { return trustScore; }
    public int getCouponCount() { return couponCount; }
    public int getPriorityPassCount() { return priorityPassCount; }
    public boolean isMarketingAgreed() { return marketingAgreed; }
    public Role getRole() { return role; }
    public List<MemberLeagueProfile> getLeagueProfiles() { return leagueProfiles; }
}
