package com.sportsmate.server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_SOCIAL_TOKEN("A401_1", "Invalid social token", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("A401_2", "Invalid refresh token", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
