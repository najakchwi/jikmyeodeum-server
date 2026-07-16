package com.sportsmate.server.domain.member.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.domain.member.policy.ProfileOptionPolicy;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("프로필 옵션 정책 테스트")
class ProfileOptionPolicyTest {

    @Test
    @DisplayName("관람 스타일은 복수 선택이며 최대 2개까지 허용한다")
    void watchStyle_selectionPolicy_hasExpectedValues() {
        assertThat(WatchStyle.MULTI_SELECTABLE).isTrue();
        assertThat(WatchStyle.MAX_COUNT).isEqualTo(2);
        assertThat(Personality.MULTI_SELECTABLE).isFalse();
        assertThat(Personality.MAX_COUNT).isNull();
        assertThat(TalkStyle.MULTI_SELECTABLE).isFalse();
        assertThat(TalkStyle.MAX_COUNT).isNull();
        assertThat(SmokingStatus.MULTI_SELECTABLE).isFalse();
        assertThat(SmokingStatus.MAX_COUNT).isNull();
    }

    @Test
    @DisplayName("비활성 관람 스타일은 활성 목록과 신규 선택 검증에서 제외된다")
    void watchStyle_inactiveOption_isExcludedAndRejected() throws Exception {
        setActive(WatchStyle.FOOD, false);
        try {
            assertThat(WatchStyle.activeValues()).doesNotContain(WatchStyle.FOOD);

            assertThatThrownBy(() -> ProfileOptionPolicy.validateWatchStyles(List.of(WatchStyle.FOOD)))
                    .isInstanceOf(BusinessException.class);
        } finally {
            setActive(WatchStyle.FOOD, true);
        }
    }

    @Test
    @DisplayName("관람 스타일 최대 개수를 넘으면 거절한다")
    void watchStyle_exceedsMaxCount_throwsException() {
        assertThatThrownBy(() -> ProfileOptionPolicy.validateWatchStyles(List.of(
                        WatchStyle.CHEER,
                        WatchStyle.FOCUS,
                        WatchStyle.ENJOY)))
                .isInstanceOf(BusinessException.class);
    }

    private static void setActive(Enum<?> option, boolean active) throws Exception {
        Field field = option.getClass().getDeclaredField("active");
        field.setAccessible(true);
        field.set(option, active);
    }
}
