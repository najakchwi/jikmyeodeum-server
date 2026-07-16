package com.sportsmate.server.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.port.out.location.KakaoLocalApiPort;
import com.sportsmate.server.common.port.out.location.LocationRegion;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    private final FakeMemberOutPort memberOutPort = new FakeMemberOutPort();
    private final MemberService memberService = new MemberService(memberOutPort, new StubKakaoLocalApiPort());

    @Test
    @DisplayName("관람 스타일 최대 개수를 넘기면 거절한다")
    void updateStyle_watchStylesExceedMaxCount_throwsException() {
        memberOutPort.save(member(1L));

        assertThatThrownBy(() -> memberService.updateStyle(
                        1L,
                        null,
                        List.of(WatchStyle.CHEER, WatchStyle.FOCUS, WatchStyle.ENJOY),
                        null,
                        null,
                        null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("비활성 옵션을 신규 선택하면 거절한다")
    void updateStyle_inactiveOption_throwsException() throws Exception {
        memberOutPort.save(member(1L));
        setActive(WatchStyle.FOOD, false);
        try {
            assertThatThrownBy(() -> memberService.updateStyle(
                            1L,
                            null,
                            List.of(WatchStyle.FOOD),
                            null,
                            null,
                            null))
                    .isInstanceOf(BusinessException.class);
        } finally {
            setActive(WatchStyle.FOOD, true);
        }
    }

    @Test
    @DisplayName("활성 옵션과 선택 정책을 만족하면 관람 성향을 수정한다")
    void updateStyle_validOptions_updatesStyle() {
        memberOutPort.save(member(1L));

        var result = memberService.updateStyle(
                1L,
                "LG",
                List.of(WatchStyle.CHEER, WatchStyle.FOOD),
                Personality.CALM,
                TalkStyle.MODERATE,
                SmokingStatus.SMOKER);

        assertThat(result.team()).isEqualTo("LG");
        assertThat(result.watchStyles()).containsExactly(WatchStyle.CHEER, WatchStyle.FOOD);
        assertThat(result.personality()).isEqualTo(Personality.CALM);
        assertThat(result.talkStyle()).isEqualTo(TalkStyle.MODERATE);
        assertThat(result.smokingStatus()).isEqualTo(SmokingStatus.SMOKER);
    }

    private Member member(Long id) {
        return Member.reconstitute(
                id,
                "01012345678",
                LocalDateTime.now(),
                "encoded",
                LoginType.PHONE,
                null,
                "야구친구",
                "",
                LocalDate.of(1997, 1, 1),
                Gender.MALE,
                null,
                "#2E7D32",
                "KIA",
                List.of(WatchStyle.CHEER),
                Personality.TENSION,
                TalkStyle.TALKATIVE,
                SmokingStatus.NON_SMOKER,
                GenderPref.ANY,
                AgePref.ANY,
                SmokingPref.ANY,
                5,
                true,
                "서울",
                37.5,
                127.0,
                0,
                0.0,
                100,
                2,
                0,
                false,
                Role.USER);
    }

    private static void setActive(Enum<?> option, boolean active) throws Exception {
        Field field = option.getClass().getDeclaredField("active");
        field.setAccessible(true);
        field.set(option, active);
    }

    private static class FakeMemberOutPort implements MemberOutPort {
        private final Map<Long, Member> members = new LinkedHashMap<>();

        @Override
        public Member save(Member member) {
            members.put(member.getId(), member);
            return member;
        }

        @Override
        public Optional<Member> findById(Long id) {
            return Optional.ofNullable(members.get(id));
        }

        @Override
        public Optional<Member> findByPhone(String phone) {
            return Optional.empty();
        }

        @Override
        public Optional<Member> findByProvider(LoginType loginType, String providerId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findExpoPushTokenById(Long id) {
            return Optional.empty();
        }

        @Override
        public boolean isWelcomeNotified(Long id) {
            return false;
        }

        @Override
        public boolean existsByPhone(String phone) {
            return false;
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return false;
        }

        @Override
        public void updateExpoPushToken(Long id, String expoPushToken) {
        }

        @Override
        public boolean markWelcomeNotified(Long id) {
            return true;
        }

        @Override
        public void withdraw(Long id) {
        }
    }

    private static class StubKakaoLocalApiPort implements KakaoLocalApiPort {
        @Override
        public LocationRegion reverseGeocode(double latitude, double longitude) {
            return new LocationRegion("서울특별시", "송파구", "잠실동");
        }
    }
}
