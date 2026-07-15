package com.sportsmate.server.common.exception;

import java.util.Map;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode, null, null, details);
    }

    public BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        this(errorCode, detail, cause, null);
    }

    public BusinessException(ErrorCode errorCode, String detail, Throwable cause, Map<String, Object> details) {
        super(detail == null ? errorCode.getMessage() : errorCode.getMessage() + " - " + detail, cause);
        this.errorCode = errorCode;
        this.details = details;
    }
}
