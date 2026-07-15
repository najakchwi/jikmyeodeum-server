package com.sportsmate.server.domain.report.exception;

import com.sportsmate.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {
    ALREADY_REPORTED("ALREADY_REPORTED", "Already reported", HttpStatus.CONFLICT),
    CANNOT_REPORT_SELF("CANNOT_REPORT_SELF", "Cannot report self", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
