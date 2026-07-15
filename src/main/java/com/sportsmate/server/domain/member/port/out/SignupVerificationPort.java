package com.sportsmate.server.domain.member.port.out;

import com.sportsmate.server.domain.member.enums.LoginType;
import java.util.Optional;

public interface SignupVerificationPort {
    String PURPOSE_SIGNUP = "signup";
    String PURPOSE_PHONE_CHANGE = "phone_change";
    String PURPOSE_PASSWORD_RESET = "password_reset";

    void saveCode(String purpose, String phone, String code, long expiresInSeconds);
    CodeStatus verifyCode(String purpose, String phone, String code);
    String issuePhoneSignupToken(String phone, long expiresInSeconds);
    String issueSocialSignupToken(String phone, LoginType loginType, String providerId, long expiresInSeconds);
    String issuePasswordResetToken(String phone, long expiresInSeconds);
    String issueSocialPendingToken(LoginType loginType, String providerId, long expiresInSeconds);
    Optional<SignupIdentity> findSocialPendingToken(String token);
    Optional<SignupIdentity> consumeSocialPendingToken(String token);
    Optional<SignupIdentity> consumeSignupToken(String token);
    Optional<String> consumePasswordResetToken(String token);

    /** 최근 발송 시도 이력을 기준으로 이번 발송 가능 여부와 남은 횟수/재시도 대기 시간을 확인한다. */
    SendAttemptStatus checkSendAttempt(String purpose, String phone);

    /** SMS 발송 성공 이력을 기록한다. 발송 성공한 시도만 호출해야 한다. */
    void recordSendAttempt(String purpose, String phone);

    default void saveCode(String phone, String code, long expiresInSeconds) {
        saveCode(PURPOSE_SIGNUP, phone, code, expiresInSeconds);
    }

    default CodeStatus verifyCode(String phone, String code) {
        return verifyCode(PURPOSE_SIGNUP, phone, code);
    }

    default SendAttemptStatus checkSendAttempt(String phone) {
        return checkSendAttempt(PURPOSE_SIGNUP, phone);
    }

    default void recordSendAttempt(String phone) {
        recordSendAttempt(PURPOSE_SIGNUP, phone);
    }

    enum CodeStatus { VALID, INVALID, EXPIRED, LOCKED }
    enum LimitReason { NONE, COOLDOWN, BURST, DAILY }
    record SignupIdentity(String phone, LoginType loginType, String providerId) {}
    record SendAttemptStatus(
            boolean allowed,
            int remainingAttempts,
            long retryAfterSeconds,
            LimitReason reason) {}
}
