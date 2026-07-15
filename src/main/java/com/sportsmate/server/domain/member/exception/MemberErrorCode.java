package com.sportsmate.server.domain.member.exception;

import com.sportsmate.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND("M404", "Member not found", HttpStatus.NOT_FOUND),
    PHONE_ALREADY_REGISTERED("M409_1", "Phone already registered", HttpStatus.CONFLICT),
    NICKNAME_ALREADY_USED("M409_2", "Nickname already used", HttpStatus.CONFLICT),
    INVALID_CODE("M400_1", "Invalid verification code", HttpStatus.BAD_REQUEST),
    CODE_EXPIRED("M400_2", "Verification code expired", HttpStatus.BAD_REQUEST),
    CODE_VERIFY_LOCKED("M400_7", "Verification code locked after too many failed attempts", HttpStatus.BAD_REQUEST),
    INVALID_SIGNUP_TOKEN("M401_1", "Invalid signup token", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("M401_2", "Invalid credentials", HttpStatus.UNAUTHORIZED),
    INVALID_RESET_TOKEN("M401_3", "Invalid or expired password reset token", HttpStatus.UNAUTHORIZED),
    INVALID_CURRENT_PASSWORD("M400_3", "Invalid current password", HttpStatus.BAD_REQUEST),
    SAME_AS_CURRENT_PASSWORD("M400_4", "New password is same as current password", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_FORMAT("M400_6", "비밀번호는 영문, 숫자를 포함해 8자 이상이어야 해요", HttpStatus.BAD_REQUEST),
    LOCATION_GEOCODING_FAILED("M400_5", "Failed to geocode location", HttpStatus.BAD_REQUEST),
    LOGIN_METHOD_NOT_FOUND("M404_2", "Login method not found", HttpStatus.NOT_FOUND),
    PHONE_LOGIN_ONLY("M403", "Phone login account only", HttpStatus.FORBIDDEN),
    SOCIAL_ACCOUNT_ALREADY_LINKED("M409_3", "Social account already linked", HttpStatus.CONFLICT),
    CANNOT_UNLINK_LAST_LOGIN_METHOD("M409_4", "Cannot unlink last login method", HttpStatus.CONFLICT),
    PHONE_NOT_VERIFIED("M409_5", "Phone verification is required", HttpStatus.CONFLICT),
    NO_PHONE_LOGIN_METHOD("M409_6", "Phone login not linked to this account", HttpStatus.CONFLICT),
    PHONE_CHANGE_LIMIT_EXCEEDED("M429_2", "Phone number can be changed once every 30 days", HttpStatus.TOO_MANY_REQUESTS),
    SMS_SEND_LIMIT_EXCEEDED("M429", "Too many verification code requests", HttpStatus.TOO_MANY_REQUESTS),
    SMS_SEND_FAILED("M502_1", "Failed to send verification SMS", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
