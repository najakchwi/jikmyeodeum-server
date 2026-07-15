package com.sportsmate.server.domain.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Member 도메인 단위 테스트")
class MemberTest {

    @Test
    @DisplayName("신규 회원은 신뢰도 100점과 온보딩 쿠폰 2개로 생성된다")
    void create_initialTrustAndCoupon_areConfigured() {
        Member member = createMember();

        assertThat(member.getTrustScore()).isEqualTo(100);
        assertThat(member.getCouponCount()).isEqualTo(2);
        assertThat(member.isLocationVerified()).isTrue();
    }

    @Test
    @DisplayName("나의 스타일과 동행 선호를 수정한다")
    void updateStyleAndPreference_changesMatchingProfile() {
        Member member = createMember();

        member.updateStyle("두산", List.of(WatchStyle.FOCUS), Personality.CALM,
                TalkStyle.QUIET, SmokingStatus.NON_SMOKER);
        member.updatePreference(GenderPref.SAME, AgePref.SIMILAR, SmokingPref.NON_SMOKER, 10);

        assertThat(member.getTeam()).isEqualTo("두산");
        assertThat(member.getWatchStyles()).containsExactly(WatchStyle.FOCUS);
        assertThat(member.getGenderPref()).isEqualTo(GenderPref.SAME);
        assertThat(member.getDistanceKm()).isEqualTo(10);
    }

    @Test
    @DisplayName("신뢰도 점수는 0점과 100점 범위를 벗어나지 않는다")
    void addTrustScore_outOfRange_isClamped() {
        Member member = createMember();

        member.addTrustScore(-150);
        assertThat(member.getTrustScore()).isZero();

        member.addTrustScore(150);
        assertThat(member.getTrustScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("쿠폰이 5개가 되면 우대권 1개로 전환된다")
    void addCoupon_reachesFive_convertsToPriorityPass() {
        Member member = createMember();

        member.addCoupon();
        member.addCoupon();
        member.addCoupon();

        assertThat(member.getCouponCount()).isZero();
        assertThat(member.getPriorityPassCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("프로필 응답은 비어있는 동행 선호를 기본값으로 보정한다")
    void memberProfileFrom_emptyPreference_usesDefaults() {
        Member member = Member.reconstitute(
                1L, "01012345678", "encoded", LoginType.PHONE, null, "트윈스러버", "소개",
                LocalDate.of(1997, 3, 15), Gender.FEMALE, null, "#2E7D32", "LG",
                List.of(WatchStyle.CHEER), Personality.TENSION, TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                null, null, null, 0, false, null, null, null,
                0, 0.0, 100, 2, 0, true, com.sportsmate.server.common.enums.Role.USER);

        MemberProfile profile = MemberProfile.from(member);

        assertThat(profile.genderPref()).isEqualTo(GenderPref.ANY);
        assertThat(profile.agePref()).isEqualTo(AgePref.ANY);
        assertThat(profile.smokingPref()).isEqualTo(SmokingPref.ANY);
        assertThat(profile.distanceKm()).isEqualTo(5);
    }

    private Member createMember() {
        return Member.create(
                "01012345678", "encoded", LoginType.PHONE, null, "트윈스러버", "소개",
                LocalDate.of(1997, 3, 15), Gender.FEMALE, "LG",
                List.of(WatchStyle.CHEER, WatchStyle.ENJOY), Personality.TENSION,
                TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.NON_SMOKER, 20, "서울 송파구 잠실동",
                37.5, 127.0, true);
    }
}
