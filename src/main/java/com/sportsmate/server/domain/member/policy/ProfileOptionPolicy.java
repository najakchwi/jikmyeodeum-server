package com.sportsmate.server.domain.member.policy;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.util.List;

public final class ProfileOptionPolicy {

    private ProfileOptionPolicy() {
    }

    public static void validateWatchStyles(List<WatchStyle> watchStyles) {
        if (watchStyles == null) {
            return;
        }
        if (!WatchStyle.MULTI_SELECTABLE && watchStyles.size() > 1) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (WatchStyle.MULTI_SELECTABLE && WatchStyle.MAX_COUNT != null
                && watchStyles.size() > WatchStyle.MAX_COUNT) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (!WatchStyle.activeValues().containsAll(watchStyles)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public static void validatePersonality(Personality personality) {
        if (personality != null && !Personality.activeValues().contains(personality)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public static void validateTalkStyle(TalkStyle talkStyle) {
        if (talkStyle != null && !TalkStyle.activeValues().contains(talkStyle)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public static void validateSmokingStatus(SmokingStatus smokingStatus) {
        if (smokingStatus != null && !SmokingStatus.activeValues().contains(smokingStatus)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public static void validateStyle(List<WatchStyle> watchStyles, Personality personality,
            TalkStyle talkStyle, SmokingStatus smokingStatus) {
        validateWatchStyles(watchStyles);
        validatePersonality(personality);
        validateTalkStyle(talkStyle);
        validateSmokingStatus(smokingStatus);
    }
}
