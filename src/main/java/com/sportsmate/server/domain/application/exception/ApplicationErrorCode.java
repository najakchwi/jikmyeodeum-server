package com.sportsmate.server.domain.application.exception;

import com.sportsmate.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ApplicationErrorCode implements ErrorCode {
    GAME_NOT_FOUND("AP404_1", "Game not found", HttpStatus.NOT_FOUND),
    APPLICATION_NOT_FOUND("AP404_2", "Application not found", HttpStatus.NOT_FOUND),
    TRUST_SCORE_TOO_LOW("AP403_1", "Trust score is too low to apply", HttpStatus.FORBIDDEN),
    ALREADY_APPLIED("AP409_1", "Already applied", HttpStatus.CONFLICT),
    APPLICATION_CLOSED("AP409_2", "Application period is closed", HttpStatus.CONFLICT),
    CANNOT_CANCEL("AP409_3", "Application cannot be cancelled", HttpStatus.CONFLICT),
    MATCH_NOT_READY("AP409_4", "Match result is not ready", HttpStatus.CONFLICT),
    CANNOT_REJECT_AFTER_CHAT_STARTED("AP409_5", "Cannot reject after chat started", HttpStatus.CONFLICT),
    ALREADY_APPLIED_ON_DATE("AP409_6", "Already applied on this date", HttpStatus.CONFLICT);
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
