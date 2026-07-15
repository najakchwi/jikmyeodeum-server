package com.sportsmate.server.domain.review.exception;

import com.sportsmate.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {
    ALREADY_REVIEWED("R409", "Already reviewed", HttpStatus.CONFLICT),
    NOT_GAME_DONE("R400", "Game is not completed", HttpStatus.BAD_REQUEST),
    INVALID_REVIEW_TAG("R400_TAG", "Invalid review tag", HttpStatus.BAD_REQUEST),
    INVALID_PROFILE_MISMATCH_FIELD("R400_PROFILE", "Invalid profile mismatch field", HttpStatus.BAD_REQUEST);
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
