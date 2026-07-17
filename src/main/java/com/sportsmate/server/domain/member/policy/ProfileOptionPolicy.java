package com.sportsmate.server.domain.member.policy;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.domain.member.enums.DrinkingStatus;
import com.sportsmate.server.domain.member.enums.FanLevel;
import com.sportsmate.server.domain.member.enums.MeetPurpose;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SeatZone;
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

    public static void validateDrinkingStatus(DrinkingStatus drinkingStatus) {
        if (drinkingStatus != null && !DrinkingStatus.activeValues().contains(drinkingStatus)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public static void validateMeetPurpose(MeetPurpose meetPurpose) {
        if (meetPurpose != null && !MeetPurpose.activeValues().contains(meetPurpose)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public static void validateFanLevel(FanLevel fanLevel) {
        if (fanLevel != null && !FanLevel.activeValues().contains(fanLevel)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public static void validateSeatZones(List<SeatZone> seatZones) {
        if (seatZones == null) {
            return;
        }
        if (!SeatZone.activeValues().containsAll(seatZones)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (seatZones.contains(SeatZone.ANY) && seatZones.size() > 1) {
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

    public static void validateStyle(Personality personality, TalkStyle talkStyle,
            SmokingStatus smokingStatus, DrinkingStatus drinkingStatus, MeetPurpose meetPurpose) {
        validatePersonality(personality);
        validateTalkStyle(talkStyle);
        validateSmokingStatus(smokingStatus);
        validateDrinkingStatus(drinkingStatus);
        validateMeetPurpose(meetPurpose);
    }

    public static void validateLeagueProfile(List<WatchStyle> watchStyles, FanLevel fanLevel,
            List<SeatZone> seatZones) {
        validateWatchStyles(watchStyles);
        validateFanLevel(fanLevel);
        validateSeatZones(seatZones);
    }
}
