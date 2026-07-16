package com.sportsmate.server.domain.application.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchCandidateFactory 단위 테스트")
class MatchCandidateFactoryTest {

    private final MatchCandidateFactory factory = new MatchCandidateFactory();

    @Test
    @DisplayName("비활성 프로필 옵션은 매칭 후보에서 제외한다")
    void from_inactiveProfileOptions_excludesFromCandidate() throws Exception {
        setActive(WatchStyle.FOOD, false);
        setActive(Personality.TENSION, false);
        setActive(TalkStyle.TALKATIVE, false);
        setActive(SmokingStatus.NON_SMOKER, false);
        try {
            MatchCandidate candidate = factory.from(Application.create("app-1", 1L, "game-1"), profile());

            assertThat(candidate.watchStyles()).containsExactly(WatchStyle.CHEER);
            assertThat(candidate.personality()).isNull();
            assertThat(candidate.talkStyle()).isNull();
            assertThat(candidate.smokingStatus()).isNull();
        } finally {
            setActive(WatchStyle.FOOD, true);
            setActive(Personality.TENSION, true);
            setActive(TalkStyle.TALKATIVE, true);
            setActive(SmokingStatus.NON_SMOKER, true);
        }
    }

    private MemberProfile profile() {
        return new MemberProfile(
                1L,
                "01012345678",
                "phone",
                "야구친구",
                "",
                LocalDate.of(1997, 1, 1),
                "male",
                null,
                "#2E7D32",
                "LG",
                0,
                0.0,
                100,
                List.of(WatchStyle.CHEER, WatchStyle.FOOD),
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
                0,
                null);
    }

    private static void setActive(Enum<?> option, boolean active) throws Exception {
        Field field = option.getClass().getDeclaredField("active");
        field.setAccessible(true);
        field.set(option, active);
    }
}
