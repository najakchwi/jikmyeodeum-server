package com.sportsmate.server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // Auth
    UNAUTHORIZED("G401", "Authentication is required", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("G403", "Access is denied", HttpStatus.FORBIDDEN),

    // Bad Request
    INVALID_INPUT("G400", "Invalid input", HttpStatus.BAD_REQUEST),

    // Rate Limit
    IP_RATE_LIMIT_EXCEEDED("G429", "Too many requests from this IP", HttpStatus.TOO_MANY_REQUESTS),

    // Server Error
    INTERNAL_SERVER_ERROR("G500", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
