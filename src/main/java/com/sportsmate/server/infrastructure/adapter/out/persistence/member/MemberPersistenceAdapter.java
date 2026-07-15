package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.infrastructure.adapter.out.persistence.game.TeamEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.game.TeamJpaRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
public class MemberPersistenceAdapter implements MemberOutPort {
    private static final GenderPref DEFAULT_GENDER_PREFERENCE = GenderPref.ANY;
    private static final AgePref DEFAULT_AGE_PREFERENCE = AgePref.ANY;
    private static final SmokingPref DEFAULT_SMOKING_PREFERENCE = SmokingPref.ANY;
    private static final int DEFAULT_DISTANCE_KM = 5;
    private static final String DEFAULT_AVATAR_COLOR = "#2E7D32";
    private static final String WITHDRAWN_NICKNAME_PREFIX = "DELETED_";

    private final MemberJpaRepository memberRepository;
    private final AuthJpaRepository authRepository;
    private final MemberPreferenceJpaRepository preferenceRepository;
    private final MemberLocationVerificationJpaRepository locationRepository;
    private final MemberStyleJpaRepository styleRepository;
    private final MemberWatchStyleJpaRepository watchStyleRepository;
    private final MemberStatsJpaRepository statsRepository;
    private final TeamJpaRepository teamRepository;

    public MemberPersistenceAdapter(
            MemberJpaRepository memberRepository,
            AuthJpaRepository authRepository,
            MemberPreferenceJpaRepository preferenceRepository,
            MemberLocationVerificationJpaRepository locationRepository,
            MemberStyleJpaRepository styleRepository,
            MemberWatchStyleJpaRepository watchStyleRepository,
            MemberStatsJpaRepository statsRepository,
            TeamJpaRepository teamRepository
    ) {
        this.memberRepository = memberRepository;
        this.authRepository = authRepository;
        this.preferenceRepository = preferenceRepository;
        this.locationRepository = locationRepository;
        this.styleRepository = styleRepository;
        this.watchStyleRepository = watchStyleRepository;
        this.statsRepository = statsRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public Member save(Member member) {
        LocalDateTime now = LocalDateTime.now();
        Optional<MemberEntity> existingMember = member.getId() == null
                ? Optional.empty()
                : memberRepository.findById(member.getId());
        AuthEntity existingAuth = existingMember
                .flatMap(existing -> authRepository.findByMemberIdAndLoginType(existing.getId(), member.getLoginType().name()))
                .or(() -> existingMember.map(MemberEntity::getAuthId).flatMap(authRepository::findById))
                .orElse(null);
        Long authId = existingAuth == null ? null : existingAuth.getId();
        AuthEntity auth = AuthEntity.builder()
                .id(authId)
                .phone(member.getLoginType() == LoginType.PHONE ? member.getPhone() : null)
                .password(member.getPassword())
                .loginType(member.getLoginType().name())
                .providerId(member.getProviderId())
                .memberId(member.getId())
                .status("ACTIVE")
                .createdAt(existingAuth == null ? now : existingAuth.getCreatedAt())
                .updatedAt(existingAuth == null ? null : now)
                .build();
        AuthEntity savedAuth = authRepository.save(auth);
        MemberEntity entity = MemberEntity.builder()
                .id(member.getId())
                .authId(savedAuth.getId())
                .phone(member.getPhone())
                .phoneVerifiedAt(member.getPhoneVerifiedAt())
                .nickname(member.getNickname())
                .birthYear(member.getBirthdate() == null ? null : member.getBirthdate().getYear())
                .gender(member.getGender() == null ? null : member.getGender().name())
                .profileImageKey(member.getAvatarUrl())
                .bio(member.getBio())
                .expoPushToken(existingMember.map(MemberEntity::getExpoPushToken).orElse(null))
                .welcomeNotified(existingMember.map(MemberEntity::isWelcomeNotified).orElse(false))
                .role(roleOrDefault(member.getRole()).name())
                .createdAt(now)
                .build();
        MemberEntity saved = memberRepository.save(entity);
        if (savedAuth.getMemberId() == null) {
            authRepository.attachMember(savedAuth.getId(), saved.getId());
            Long savedAuthId = savedAuth.getId();
            savedAuth = authRepository.findById(savedAuth.getId())
                    .orElseThrow(() -> new IllegalStateException("Auth not found: " + savedAuthId));
        }

        preferenceRepository.save(MemberPreferenceEntity.builder()
                .memberId(saved.getId())
                .genderPref(genderPreferenceOrDefault(member.getGenderPref()))
                .agePref(agePreferenceOrDefault(member.getAgePref()))
                .smokingPref(smokingPreferenceOrDefault(member.getSmokingPref()))
                .distanceKm(distanceOrDefault(member.getDistanceKm()))
                .build());

        Long favoriteTeamId = resolveTeamId(member.getTeam());
        styleRepository.save(MemberStyleEntity.builder()
                .memberId(saved.getId())
                .favoriteTeamId(favoriteTeamId)
                .personality(member.getPersonality())
                .talkStyle(member.getTalkStyle())
                .smokingStatus(member.getSmokingStatus())
                .build());

        watchStyleRepository.deleteAllByMemberId(saved.getId());
        List<WatchStyle> watchStyles = member.getWatchStyles();
        if (watchStyles != null) {
            for (WatchStyle style : watchStyles) {
                watchStyleRepository.save(MemberWatchStyleEntity.builder()
                        .id(MemberWatchStyleId.of(style, saved.getId()))
                        .build());
            }
        }

        statsRepository.save(MemberStatsEntity.builder()
                .memberId(saved.getId())
                .matchCount(member.getMatchCount())
                .rating(member.getRating())
                .trustScore(member.getTrustScore())
                .couponCount(member.getCouponCount())
                .priorityPassCount(member.getPriorityPassCount())
                .build());

        if (member.isLocationVerified() && member.getLatitude() != null) {
            locationRepository.save(MemberLocationVerificationEntity.builder()
                    .memberId(saved.getId())
                    .verified(true)
                    .address(member.getLocationAddress())
                    .latitude(member.getLatitude())
                    .longitude(member.getLongitude())
                    .verifiedAt(now)
                    .build());
        }
        return toDomain(saved, savedAuth);
    }

    @Override public Optional<Member> findById(Long id) { return memberRepository.findById(id).map(this::toDomain); }
    @Override public Optional<Member> findByPhone(String phone) { return memberRepository.findByPhone(phone).map(this::toDomain); }
    @Override public Optional<Member> findByProvider(LoginType type, String providerId) {
        return memberRepository.findByLoginTypeAndProviderId(type, providerId).map(this::toDomain);
    }
    @Override public Optional<String> findExpoPushTokenById(Long id) {
        return memberRepository.findExpoPushTokenById(id);
    }
    @Override public boolean isWelcomeNotified(Long id) { return memberRepository.isWelcomeNotified(id); }
    @Override public boolean existsByPhone(String phone) { return memberRepository.existsByPhone(phone); }
    @Override public boolean existsByNickname(String nickname) { return memberRepository.existsByNickname(nickname); }
    @Override public void updateExpoPushToken(Long id, String expoPushToken) {
        memberRepository.updateExpoPushToken(id, expoPushToken);
    }
    @Override public boolean markWelcomeNotified(Long id) {
        return memberRepository.markWelcomeNotified(id) > 0;
    }

    @Override
    public void withdraw(Long id) {
        MemberEntity member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Member not found: " + id));
        memberRepository.anonymize(id, withdrawnNickname());
        authRepository.findAllByMemberIdAndStatus(id, "ACTIVE")
                .forEach(auth -> authRepository.withdraw(auth.getId()));
        if (member.getAuthId() != null) {
            authRepository.withdraw(member.getAuthId());
        }
        styleRepository.deleteByMemberId(id);
        preferenceRepository.deleteByMemberId(id);
        locationRepository.deleteByMemberId(id);
        watchStyleRepository.deleteAllByMemberId(id);
    }

    private String withdrawnNickname() {
        return WITHDRAWN_NICKNAME_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    @Override
    public List<LinkedAccount> findLinkedAccounts(Long memberId) {
        List<AuthEntity> auths = authRepository.findAllByMemberIdAndStatus(memberId, "ACTIVE");
        List<LinkedAccount> result = new ArrayList<>();
        for (LoginType loginType : LoginType.values()) {
            Optional<AuthEntity> auth = auths.stream()
                    .filter(item -> item.getLoginType().equals(loginType.name()))
                    .findFirst();
            result.add(new LinkedAccount(
                    loginType,
                    auth.isPresent(),
                    auth.map(AuthEntity::getCreatedAt).orElse(null)));
        }
        return result;
    }

    @Override
    public Optional<Long> findLinkedMemberId(LoginType loginType, String providerId) {
        return authRepository.findByLoginTypeAndProviderId(loginType.name(), providerId)
                .map(AuthEntity::getMemberId);
    }

    @Override
    public boolean hasLoginMethod(Long memberId, LoginType loginType) {
        return authRepository.existsByMemberIdAndLoginTypeAndStatus(memberId, loginType.name(), "ACTIVE");
    }

    @Override
    public int countLoginMethods(Long memberId) {
        return authRepository.countByMemberIdAndStatus(memberId, "ACTIVE");
    }

    @Override
    public void linkSocialAccount(Long memberId, LoginType loginType, String providerId) {
        LocalDateTime now = LocalDateTime.now();
        authRepository.save(AuthEntity.builder()
                .memberId(memberId)
                .loginType(loginType.name())
                .providerId(providerId)
                .status("ACTIVE")
                .createdAt(now)
                .build());
    }

    @Override
    public void linkPhoneAccount(Long memberId, String encodedPassword) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("Member not found: " + memberId));
        LocalDateTime now = LocalDateTime.now();
        authRepository.save(AuthEntity.builder()
                .memberId(memberId)
                .phone(member.getPhone())
                .password(encodedPassword)
                .loginType(LoginType.PHONE.name())
                .status("ACTIVE")
                .createdAt(now)
                .build());
    }

    @Override
    public void unlinkLoginMethod(Long memberId, LoginType loginType) {
        authRepository.deleteByMemberIdAndLoginType(memberId, loginType.name());
    }

    @Override
    public void changePhone(Long memberId, String phone) {
        memberRepository.updatePhone(memberId, phone);
    }

    private Member toDomain(MemberEntity entity) {
        AuthEntity auth = primaryAuth(entity)
                .orElseThrow(() -> new IllegalStateException("Auth not found for member: " + entity.getId()));
        return toDomain(entity, auth);
    }

    private Optional<AuthEntity> primaryAuth(MemberEntity entity) {
        if (entity.getAuthId() != null) {
            Optional<AuthEntity> legacyAuth = authRepository.findById(entity.getAuthId());
            if (legacyAuth.isPresent()) {
                return legacyAuth;
            }
        }
        return authRepository.findAllByMemberIdAndStatus(entity.getId(), "ACTIVE").stream().findFirst();
    }

    private Member toDomain(MemberEntity entity, AuthEntity auth) {
        MemberPreferenceEntity preference = preferenceRepository.findById(entity.getId()).orElse(null);
        MemberLocationVerificationEntity location = locationRepository.findById(entity.getId()).orElse(null);
        MemberStyleEntity style = styleRepository.findById(entity.getId()).orElse(null);
        List<WatchStyle> watchStyles = watchStyleRepository.findAllByMemberId(entity.getId())
                .stream().map(w -> w.getId().getWatchStyle()).toList();
        MemberStatsEntity stats = statsRepository.findById(entity.getId()).orElse(null);

        boolean locationVerified = location != null && Boolean.TRUE.equals(location.getVerified());

        String team = null;
        if (style != null && style.getFavoriteTeamId() != null) {
            team = teamRepository.findById(style.getFavoriteTeamId())
                    .map(TeamEntity::getShortName).orElse(null);
        }

        return Member.reconstitute(
                entity.getId(),
                entity.getPhone() == null ? auth.getPhone() : entity.getPhone(),
                entity.getPhoneVerifiedAt(),
                auth.getPassword(),
                LoginType.valueOf(auth.getLoginType()),
                auth.getProviderId(),
                entity.getNickname(),
                entity.getBio(),
                entity.getBirthYear() == null ? null : java.time.LocalDate.of(entity.getBirthYear(), 1, 1),
                entity.getGender() == null ? null : com.sportsmate.server.domain.member.enums.Gender.valueOf(entity.getGender()),
                entity.getProfileImageKey(),
                DEFAULT_AVATAR_COLOR,
                team,
                watchStyles,
                style == null ? null : style.getPersonality(),
                style == null ? null : style.getTalkStyle(),
                style == null ? null : style.getSmokingStatus(),
                preference == null ? DEFAULT_GENDER_PREFERENCE : genderPreferenceOrDefault(preference.getGenderPref()),
                preference == null ? DEFAULT_AGE_PREFERENCE : agePreferenceOrDefault(preference.getAgePref()),
                preference == null ? DEFAULT_SMOKING_PREFERENCE : smokingPreferenceOrDefault(preference.getSmokingPref()),
                preference == null ? DEFAULT_DISTANCE_KM : distanceOrDefault(preference.getDistanceKm()),
                locationVerified,
                location != null ? location.getAddress() : null,
                location != null ? location.getLatitude() : null,
                location != null ? location.getLongitude() : null,
                stats == null ? 0 : stats.getMatchCount(),
                stats == null ? 0.0 : stats.getRating(),
                stats == null ? 0 : stats.getTrustScore(),
                stats == null ? 0 : stats.getCouponCount(),
                stats == null ? 0 : stats.getPriorityPassCount(),
                false,
                roleOrDefault(entity.getRole()));
    }

    private Role roleOrDefault(String role) {
        return role == null || role.isBlank() ? Role.USER : Role.valueOf(role);
    }

    private Role roleOrDefault(Role role) {
        return role == null ? Role.USER : role;
    }

    private Long resolveTeamId(String teamShortName) {
        if (teamShortName == null || teamShortName.isBlank()) return null;
        return teamRepository.findByShortName(teamShortName)
                .map(TeamEntity::getId).orElse(null);
    }

    private GenderPref genderPreferenceOrDefault(GenderPref preference) {
        return preference == null ? DEFAULT_GENDER_PREFERENCE : preference;
    }

    private AgePref agePreferenceOrDefault(AgePref preference) {
        return preference == null ? DEFAULT_AGE_PREFERENCE : preference;
    }

    private SmokingPref smokingPreferenceOrDefault(SmokingPref preference) {
        return preference == null ? DEFAULT_SMOKING_PREFERENCE : preference;
    }

    private int distanceOrDefault(Integer distanceKm) {
        return distanceKm == null || distanceKm == 0 ? DEFAULT_DISTANCE_KM : distanceKm;
    }
}
