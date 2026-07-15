package com.sportsmate.server.domain.chat.exception;

import com.sportsmate.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {
    CHAT_NOT_FOUND("C404", "Chat not found", HttpStatus.NOT_FOUND),
    CHAT_CLOSED("C409", "Chat is closed", HttpStatus.CONFLICT);
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
