package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WithdrawalReason;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.out.MemberWithdrawalLogPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({MemberPersistenceAdapter.class, MemberWithdrawalLogPersistenceAdapter.class})
@DisplayName("MemberPersistenceAdapter JPA 테스트")
class MemberPersistenceAdapterTest {

    @Autowired
    MemberPersistenceAdapter adapter;

    @Autowired
    MemberJpaRepository memberRepository;

    @Autowired
    MemberWithdrawalLogPersistenceAdapter withdrawalLogAdapter;

    @Autowired
    MemberWithdrawalLogJpaRepository withdrawalLogRepository;

    @Test
    @DisplayName("소셜 회원은 휴대폰 번호 없이 저장하고 제공자 식별자로 조회할 수 있다")
    void save_socialMemberWithoutPhone_findByProvider() {
        Member member = Member.create(
                null, null, LoginType.KAKAO, "kakao-123", "카카오회원", "소개",
                LocalDate.of(1998, 5, 20), Gender.MALE, "LG",
                List.of(WatchStyle.CHEER), Personality.TENSION, TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.NON_SMOKER, 20, "서울 송파구 잠실동",
                37.5, 127.0, true);

        Member saved = adapter.save(member);
        Member found = adapter.findByProvider(LoginType.KAKAO, "kakao-123").orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(found.getPhone()).isNull();
        assertThat(found.getPassword()).isNull();
        assertThat(found.getLoginType()).isEqualTo(LoginType.KAKAO);
        assertThat(found.getProviderId()).isEqualTo("kakao-123");
        assertThat(found.getNickname()).isEqualTo("카카오회원");
        assertThat(found.getGenderPref()).isEqualTo(GenderPref.ANY);
        assertThat(found.getAgePref()).isEqualTo(AgePref.ANY);
        assertThat(found.getSmokingPref()).isEqualTo(SmokingPref.NON_SMOKER);
        assertThat(found.getDistanceKm()).isEqualTo(20);
    }

    @Test
    @DisplayName("휴대폰 회원은 저장 후 휴대폰 번호로 조회할 수 있다")
    void save_phoneMember_findByPhone() {
        Member member = Member.create(
                "01012345678", "encoded-password", LoginType.PHONE, null, "휴대폰회원", "소개",
                LocalDate.of(1997, 3, 15), Gender.FEMALE, "LG",
                List.of(WatchStyle.CHEER), Personality.TENSION, TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.NON_SMOKER, 20, "서울 송파구 잠실동",
                37.5, 127.0, true);

        adapter.save(member);
        Member found = adapter.findByPhone("01012345678").orElseThrow();

        assertThat(found.getPhone()).isEqualTo("01012345678");
        assertThat(found.getPassword()).isEqualTo("encoded-password");
        assertThat(found.getLoginType()).isEqualTo(LoginType.PHONE);
    }

    @Test
    @DisplayName("동행 선호를 수정하면 저장 후 조회에도 반영된다")
    void save_updatedPreference_persistsPreference() {
        Member member = Member.create(
                "01087654321", "encoded-password", LoginType.PHONE, null, "선호수정회원", "소개",
                LocalDate.of(1997, 3, 15), Gender.FEMALE, "LG",
                List.of(WatchStyle.CHEER), Personality.TENSION, TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, "서울 송파구 잠실동",
                37.5, 127.0, true);
        Member saved = adapter.save(member);

        saved.updatePreference(GenderPref.SAME, AgePref.SIMILAR, SmokingPref.NON_SMOKER, 10);
        adapter.save(saved);

        Member found = adapter.findByPhone("01087654321").orElseThrow();
        assertThat(found.getGenderPref()).isEqualTo(GenderPref.SAME);
        assertThat(found.getAgePref()).isEqualTo(AgePref.SIMILAR);
        assertThat(found.getSmokingPref()).isEqualTo(SmokingPref.NON_SMOKER);
        assertThat(found.getDistanceKm()).isEqualTo(10);
    }

    @Test
    @DisplayName("푸시 토큰은 별도로 등록하고 회원 정보 저장 후에도 유지된다")
    void updateExpoPushToken_thenSaveMember_preservesToken() {
        Member member = Member.create(
                "01011112222", "encoded-password", LoginType.PHONE, null, "푸시회원", "소개",
                LocalDate.of(1997, 3, 15), Gender.FEMALE, "LG",
                List.of(WatchStyle.CHEER), Personality.TENSION, TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, "서울 송파구 잠실동",
                37.5, 127.0, true);
        Member saved = adapter.save(member);
        adapter.updateExpoPushToken(saved.getId(), "ExponentPushToken[test]");

        saved.updatePreference(GenderPref.SAME, AgePref.SIMILAR, SmokingPref.NON_SMOKER, 10);
        adapter.save(saved);

        assertThat(adapter.findExpoPushTokenById(saved.getId()))
                .contains("ExponentPushToken[test]");
    }

    @Test
    @DisplayName("환영 알림 발송 여부는 한 번만 표시할 수 있고 회원 정보 저장 후에도 유지된다")
    void markWelcomeNotified_thenSaveMember_preservesFlag() {
        Member member = Member.create(
                "01011113333", "encoded-password", LoginType.PHONE, null, "환영회원", "소개",
                LocalDate.of(1997, 3, 15), Gender.FEMALE, "LG",
                List.of(WatchStyle.CHEER), Personality.TENSION, TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, "서울 송파구 잠실동",
                37.5, 127.0, true);
        Member saved = adapter.save(member);

        assertThat(adapter.isWelcomeNotified(saved.getId())).isFalse();
        assertThat(adapter.markWelcomeNotified(saved.getId())).isTrue();
        assertThat(adapter.markWelcomeNotified(saved.getId())).isFalse();

        saved.updatePreference(GenderPref.SAME, AgePref.SIMILAR, SmokingPref.NON_SMOKER, 10);
        adapter.save(saved);

        assertThat(adapter.isWelcomeNotified(saved.getId())).isTrue();
    }

    @Test
    @DisplayName("탈퇴 처리하면 개인정보가 파기되고 휴대폰/제공자 식별자로는 더 이상 조회되지 않는다")
    void withdraw_anonymizesMemberAndAuth() {
        Member member = Member.create(
                "01099998888", "encoded-password", LoginType.PHONE, null, "탈퇴테스트회원", "소개",
                LocalDate.of(1997, 3, 15), Gender.FEMALE, "LG",
                List.of(WatchStyle.CHEER), Personality.TENSION, TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, "서울 송파구 잠실동",
                37.5, 127.0, true);
        Member saved = adapter.save(member);

        adapter.withdraw(saved.getId());

        Member found = adapter.findById(saved.getId()).orElseThrow();
        MemberEntity entity = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getNickname()).startsWith("DELETED_").hasSize(14);
        assertThat(found.getPhone()).isEqualTo("DELETED");
        assertThat(found.getPassword()).isNull();
        assertThat(found.getGender()).isNull();
        assertThat(found.getBio()).isNull();
        assertThat(found.getTeam()).isNull();
        assertThat(found.getWatchStyles()).isEmpty();
        assertThat(found.isLocationVerified()).isFalse();
        assertThat(entity.getDeletedAt()).isNotNull();
        assertThat(adapter.findByPhone("01099998888")).isEmpty();
    }

    @Test
    @DisplayName("회원 탈퇴 이력을 원본 전화번호와 닉네임, 사유와 함께 저장한다")
    void saveWithdrawalLog_success() {
        LocalDateTime withdrawnAt = LocalDateTime.now();

        withdrawalLogAdapter.save(new MemberWithdrawalLogPort.WithdrawalLog(
                1L,
                "01012345678",
                "탈퇴전닉네임",
                WithdrawalReason.OTHER,
                "기타 사유",
                withdrawnAt));

        MemberWithdrawalLogEntity log = withdrawalLogRepository.findByMemberId(1L).orElseThrow();
        assertThat(log.getPhone()).isEqualTo("01012345678");
        assertThat(log.getNickname()).isEqualTo("탈퇴전닉네임");
        assertThat(log.getReason()).isEqualTo("OTHER");
        assertThat(log.getReasonDetail()).isEqualTo("기타 사유");
        assertThat(log.getWithdrawnAt()).isEqualTo(withdrawnAt);
    }
}
