package com.sportsmate.server.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.common.port.out.audit.AuditResult;
import com.sportsmate.server.common.port.out.location.KakaoLocalApiPort;
import com.sportsmate.server.common.port.out.oauth.GoogleAuthPort;
import com.sportsmate.server.common.port.out.oauth.KakaoAuthPort;
import com.sportsmate.server.common.port.out.oauth.SocialUserInfo;
import com.sportsmate.server.common.port.out.sms.SmsSender;
import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.token.TokenIssuer;
import com.sportsmate.server.common.port.out.token.TokenStore;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.application.port.in.ApplicationUseCase;
import com.sportsmate.server.domain.member.enums.WithdrawalReason;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.exception.MemberErrorCode;
import com.sportsmate.server.domain.member.port.in.AuthUseCase;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.member.port.out.MemberWithdrawalLogPort;
import com.sportsmate.server.domain.member.port.out.PasswordHasher;
import com.sportsmate.server.domain.member.port.out.PhoneChangeLogPort;
import com.sportsmate.server.domain.member.port.out.SignupVerificationPort;
import com.sportsmate.server.domain.policy.port.in.PolicyUseCase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    private final MemberOutPort memberOutPort = Mockito.mock(MemberOutPort.class);
    private final MemberWithdrawalLogPort memberWithdrawalLogPort = Mockito.mock(MemberWithdrawalLogPort.class);
    private final PhoneChangeLogPort phoneChangeLogPort = Mockito.mock(PhoneChangeLogPort.class);
    private final ApplicationUseCase applicationUseCase = Mockito.mock(ApplicationUseCase.class);
    private final SignupVerificationPort signupVerificationPort = Mockito.mock(SignupVerificationPort.class);
    private final SmsSender smsSender = Mockito.mock(SmsSender.class);
    private final TokenIssuer tokenIssuer = Mockito.mock(TokenIssuer.class);
    private final TokenStore tokenStore = Mockito.mock(TokenStore.class);
    private final PasswordHasher passwordHasher = Mockito.mock(PasswordHasher.class);
    private final KakaoAuthPort kakaoAuthPort = Mockito.mock(KakaoAuthPort.class);
    private final GoogleAuthPort googleAuthPort = Mockito.mock(GoogleAuthPort.class);
    private final KakaoLocalApiPort kakaoLocalApi = Mockito.mock(KakaoLocalApiPort.class);
    private final PolicyUseCase policyUseCase = Mockito.mock(PolicyUseCase.class);
    private final ObjectStorage objectStorage = Mockito.mock(ObjectStorage.class);
    private final AuditLogPort auditLogPort = Mockito.mock(AuditLogPort.class);

    private final AuthService authService = new AuthService(
            memberOutPort, memberWithdrawalLogPort, phoneChangeLogPort, applicationUseCase, signupVerificationPort, smsSender,
            tokenIssuer, tokenStore, passwordHasher, kakaoAuthPort, googleAuthPort, kakaoLocalApi, policyUseCase,
            objectStorage, auditLogPort, "", 2592000L);

    @Test
    @DisplayName("탈퇴 처리하면 개인정보를 파기하고 refreshToken을 폐기하고 아바타를 삭제한다")
    void withdraw_existingMember_withdrawsRevokesTokenAndDeletesAvatar() {
        Member member = aMember(1L, "https://cdn.example.com/avatars/1/profile.png");
        when(memberOutPort.findById(1L)).thenReturn(Optional.of(member));
        when(objectStorage.extractKey("https://cdn.example.com/avatars/1/profile.png"))
                .thenReturn("avatars/1/profile.png");

        authService.withdraw(1L, WithdrawalReason.UNSATISFIED_MATCHING, null);

        verify(memberWithdrawalLogPort).save(argThat(log ->
                log.memberId().equals(1L)
                        && log.phone().equals("01012341")
                        && log.nickname().equals("회원1")
                        && log.reason() == WithdrawalReason.UNSATISFIED_MATCHING
                        && log.reasonDetail() == null
                        && log.withdrawnAt() != null));
        verify(applicationUseCase).cancelAllActiveByMember(1L);
        verify(memberOutPort).withdraw(1L);
        verify(tokenStore).deleteByMemberId("1");
        verify(objectStorage).delete("avatars/1/profile.png");
    }

    @Test
    @DisplayName("아바타가 없는 회원을 탈퇴 처리하면 스토리지 삭제를 호출하지 않는다")
    void withdraw_memberWithoutAvatar_skipsObjectStorageDelete() {
        Member member = aMember(2L, null);
        when(memberOutPort.findById(2L)).thenReturn(Optional.of(member));

        authService.withdraw(2L, WithdrawalReason.LOW_USAGE, null);

        verify(memberOutPort).withdraw(2L);
        verify(tokenStore).deleteByMemberId("2");
        verify(objectStorage, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 회원을 탈퇴 처리하면 예외가 발생하고 탈퇴/토큰 폐기를 호출하지 않는다")
    void withdraw_memberNotFound_throwsException() {
        when(memberOutPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.withdraw(99L, WithdrawalReason.LOW_USAGE, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);

        verify(memberWithdrawalLogPort, never()).save(any());
        verify(memberOutPort, never()).withdraw(any());
        verify(tokenStore, never()).deleteByMemberId(any());
    }

    @Test
    @DisplayName("로그인에 성공하면 감사 로그에 LOGIN_SUCCESS를 남긴다")
    void login_success_recordsAuditLog() {
        Member member = aMember(3L, null);
        when(memberOutPort.findByPhone("01012343")).thenReturn(Optional.of(member));
        when(passwordHasher.matches("password", "encoded-password")).thenReturn(true);
        when(tokenIssuer.issue(any(), any()))
                .thenReturn(new com.sportsmate.server.common.port.out.token.TokenPair("access", "refresh"));

        authService.login("01012343", "password");

        verify(auditLogPort).record(argThat(event ->
                event.category() == AuditCategory.AUTH_LOGIN
                        && event.action().equals("LOGIN_SUCCESS")
                        && event.actorId().equals("3")
                        && event.result() == AuditResult.SUCCESS));
    }

    @Test
    @DisplayName("로그인에 실패하면 감사 로그에 LOGIN_FAILED를 남기고 예외가 발생한다")
    void login_failure_recordsAuditLogAndThrows() {
        when(memberOutPort.findByPhone("01099999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("01099999", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_CREDENTIALS);

        verify(auditLogPort).record(argThat(event ->
                event.category() == AuditCategory.AUTH_LOGIN
                        && event.action().equals("LOGIN_FAILED")
                        && event.actorId() == null
                        && event.result() == AuditResult.FAILURE));
    }

    @Test
    @DisplayName("인증번호 발송에 성공하면 코드를 저장하고 발송 이력을 기록한다")
    void sendSignupCode_success_savesCodeAndRecordsAttempt() {
        when(memberOutPort.existsByPhone("01011112222")).thenReturn(false);
        when(signupVerificationPort.checkSendAttempt(SignupVerificationPort.PURPOSE_SIGNUP, "01011112222"))
                .thenReturn(new SignupVerificationPort.SendAttemptStatus(
                        true, 3, 0, SignupVerificationPort.LimitReason.NONE));

        AuthUseCase.SendCodeResult result = authService.sendSignupCode("01011112222");

        assertThat(result.expiresInSeconds()).isEqualTo(180);
        assertThat(result.remainingAttempts()).isEqualTo(2);
        verify(signupVerificationPort).saveCode(eq(SignupVerificationPort.PURPOSE_SIGNUP),
                eq("01011112222"), any(), Mockito.anyLong());
        verify(signupVerificationPort).recordSendAttempt(SignupVerificationPort.PURPOSE_SIGNUP, "01011112222");
    }

    @Test
    @DisplayName("10분 내 발송 한도를 초과하면 예외가 발생하고 SMS를 발송하지 않는다")
    void sendSignupCode_rateLimitExceeded_throwsAndSkipsSms() {
        when(memberOutPort.existsByPhone("01033334444")).thenReturn(false);
        when(signupVerificationPort.checkSendAttempt(SignupVerificationPort.PURPOSE_SIGNUP, "01033334444"))
                .thenReturn(new SignupVerificationPort.SendAttemptStatus(
                        false, 0, 120, SignupVerificationPort.LimitReason.BURST));

        assertThatThrownBy(() -> authService.sendSignupCode("01033334444"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.SMS_SEND_LIMIT_EXCEEDED)
                .extracting("details")
                .satisfies(details -> {
                    assertThat(((java.util.Map<?, ?>) details).get("retryAfterSeconds")).isEqualTo(120L);
                    assertThat(((java.util.Map<?, ?>) details).get("reason")).isEqualTo("BURST");
                });

        verify(smsSender, never()).send(any(), any());
        verify(signupVerificationPort, never()).saveCode(any(), any(), any(), Mockito.anyLong());
        verify(signupVerificationPort, never()).recordSendAttempt(any(), any());
    }

    @Test
    @DisplayName("SMS 발송 업체(Solapi) 호출이 실패하면 예외가 발생하고 발송 이력에 포함하지 않는다")
    void sendSignupCode_smsSendFails_throwsAndDoesNotCountAttempt() {
        when(memberOutPort.existsByPhone("01055556666")).thenReturn(false);
        when(signupVerificationPort.checkSendAttempt(SignupVerificationPort.PURPOSE_SIGNUP, "01055556666"))
                .thenReturn(new SignupVerificationPort.SendAttemptStatus(
                        true, 3, 0, SignupVerificationPort.LimitReason.NONE));
        Mockito.doThrow(new RuntimeException("solapi error"))
                .when(smsSender).send(any(), any());

        assertThatThrownBy(() -> authService.sendSignupCode("01055556666"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.SMS_SEND_FAILED);

        verify(signupVerificationPort, never()).saveCode(any(), any(), any(), Mockito.anyLong());
        verify(signupVerificationPort, never()).recordSendAttempt(any(), any());
    }

    @Test
    @DisplayName("60초 재전송 쿨다운에 걸리면 COOLDOWN 사유를 내려준다")
    void sendSignupCode_cooldownExceeded_includesCooldownReason() {
        when(memberOutPort.existsByPhone("01066667777")).thenReturn(false);
        when(signupVerificationPort.checkSendAttempt(SignupVerificationPort.PURPOSE_SIGNUP, "01066667777"))
                .thenReturn(new SignupVerificationPort.SendAttemptStatus(
                        false, 0, 45, SignupVerificationPort.LimitReason.COOLDOWN));

        assertThatThrownBy(() -> authService.sendSignupCode("01066667777"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.SMS_SEND_LIMIT_EXCEEDED)
                .extracting("details")
                .satisfies(details -> {
                    assertThat(((java.util.Map<?, ?>) details).get("retryAfterSeconds")).isEqualTo(45L);
                    assertThat(((java.util.Map<?, ?>) details).get("reason")).isEqualTo("COOLDOWN");
                });

        verify(smsSender, never()).send(any(), any());
    }

    @Test
    @DisplayName("일일 발송 상한에 걸리면 DAILY 사유를 내려준다")
    void sendSignupCode_dailyLimitExceeded_includesDailyReason() {
        when(memberOutPort.existsByPhone("01077778888")).thenReturn(false);
        when(signupVerificationPort.checkSendAttempt(SignupVerificationPort.PURPOSE_SIGNUP, "01077778888"))
                .thenReturn(new SignupVerificationPort.SendAttemptStatus(
                        false, 0, 3600, SignupVerificationPort.LimitReason.DAILY));

        assertThatThrownBy(() -> authService.sendSignupCode("01077778888"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.SMS_SEND_LIMIT_EXCEEDED)
                .extracting("details")
                .satisfies(details -> {
                    assertThat(((java.util.Map<?, ?>) details).get("retryAfterSeconds")).isEqualTo(3600L);
                    assertThat(((java.util.Map<?, ?>) details).get("reason")).isEqualTo("DAILY");
                });

        verify(smsSender, never()).send(any(), any());
    }

    @Test
    @DisplayName("비밀번호 재설정 인증번호는 전화번호 로그인 회원에게만 발송한다")
    void sendResetPasswordCode_socialOnlyPhone_throwsNoPhoneLoginMethod() {
        Member member = Member.reconstitute(
                11L, "01088889999", null, LoginType.KAKAO, "kakao-provider-id",
                "회원11", "소개", LocalDate.of(1997, 3, 15), Gender.MALE, null,
                "#2E7D32", "LG", List.of(WatchStyle.CHEER), Personality.TENSION,
                TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, false, "서울 송파구 잠실동",
                37.5, 127.0, 0, 0.0, 100, 2, 0, true, Role.USER);
        when(memberOutPort.findByPhone("01088889999")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> authService.sendResetPasswordCode("01088889999"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.NO_PHONE_LOGIN_METHOD);

        verify(signupVerificationPort, never()).checkSendAttempt(any(), any());
        verify(smsSender, never()).send(any(), any());
    }

    @Test
    @DisplayName("비밀번호 재설정 인증번호 발송에 성공하면 password_reset purpose로 저장한다")
    void sendResetPasswordCode_success_savesPasswordResetCode() {
        when(memberOutPort.findByPhone("01011112222")).thenReturn(Optional.of(aMember(1L, null)));
        when(signupVerificationPort.checkSendAttempt(SignupVerificationPort.PURPOSE_PASSWORD_RESET, "01011112222"))
                .thenReturn(new SignupVerificationPort.SendAttemptStatus(
                        true, 3, 0, SignupVerificationPort.LimitReason.NONE));

        AuthUseCase.SendCodeResult result = authService.sendResetPasswordCode("01011112222");

        assertThat(result.expiresInSeconds()).isEqualTo(180);
        assertThat(result.remainingAttempts()).isEqualTo(2);
        verify(signupVerificationPort).saveCode(eq(SignupVerificationPort.PURPOSE_PASSWORD_RESET),
                eq("01011112222"), any(), Mockito.anyLong());
        verify(signupVerificationPort).recordSendAttempt(
                SignupVerificationPort.PURPOSE_PASSWORD_RESET, "01011112222");
    }

    @Test
    @DisplayName("비밀번호 재설정 인증번호 검증에 성공하면 resetToken을 발급한다")
    void verifyResetPasswordCode_success_issuesResetToken() {
        when(signupVerificationPort.verifyCode(
                SignupVerificationPort.PURPOSE_PASSWORD_RESET, "01011112222", "123456"))
                .thenReturn(SignupVerificationPort.CodeStatus.VALID);
        when(memberOutPort.findByPhone("01011112222")).thenReturn(Optional.of(aMember(1L, null)));
        when(signupVerificationPort.issuePasswordResetToken("01011112222", 900))
                .thenReturn("reset-token");

        String resetToken = authService.verifyResetPasswordCode("01011112222", "123456");

        assertThat(resetToken).isEqualTo("reset-token");
    }

    @Test
    @DisplayName("유효하지 않은 resetToken으로 비밀번호 재설정 시 전용 예외가 발생한다")
    void resetPassword_invalidToken_throwsInvalidResetToken() {
        when(signupVerificationPort.consumePasswordResetToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("bad-token", "newPassword123"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_RESET_TOKEN);

        verify(memberOutPort, never()).save(any());
        verify(tokenStore, never()).deleteByMemberId(any());
    }

    @Test
    @DisplayName("비밀번호 재설정에 성공하면 새 비밀번호를 저장하고 refreshToken을 폐기한다")
    void resetPassword_success_changesPasswordAndRevokesRefreshToken() {
        Member member = aMember(1L, null);
        when(signupVerificationPort.consumePasswordResetToken("reset-token"))
                .thenReturn(Optional.of("01011112222"));
        when(memberOutPort.findByPhone("01011112222")).thenReturn(Optional.of(member));
        when(passwordHasher.matches("newPassword123", "encoded-password")).thenReturn(false);
        when(passwordHasher.hash("newPassword123")).thenReturn("encoded-new-password");

        authService.resetPassword("reset-token", "newPassword123");

        assertThat(member.getPassword()).isEqualTo("encoded-new-password");
        verify(memberOutPort).save(member);
        verify(tokenStore).deleteByMemberId("1");
    }

    @Test
    @DisplayName("신규 소셜 로그인은 최종 signupToken이 아닌 socialPendingToken을 반환한다")
    void socialLogin_newUser_returnsSocialPendingToken() {
        when(kakaoAuthPort.verify("kakao-token"))
                .thenReturn(new SocialUserInfo("kakao-provider-id", null, null));
        when(memberOutPort.findByProvider(LoginType.KAKAO, "kakao-provider-id"))
                .thenReturn(Optional.empty());
        when(signupVerificationPort.issueSocialPendingToken(LoginType.KAKAO, "kakao-provider-id", 600))
                .thenReturn("pending-token");

        AuthUseCase.SocialAuthResult result = authService.socialLogin("kakao", "kakao-token");

        assertThat(result.isNewUser()).isTrue();
        assertThat(result.socialPendingToken()).isEqualTo("pending-token");
        assertThat(result.accessToken()).isNull();
    }

    @Test
    @DisplayName("회원가입 비밀번호가 값이 있으면 영문과 숫자를 포함한 8자 이상이어야 한다")
    void signup_withInvalidPassword_throwsInvalidPasswordFormat() {
        when(signupVerificationPort.consumeSignupToken("signup-token"))
                .thenReturn(Optional.of(new SignupVerificationPort.SignupIdentity(
                        "01011112222", LoginType.PHONE, null)));
        when(memberOutPort.existsByPhone("01011112222")).thenReturn(false);
        when(memberOutPort.existsByNickname("회원1")).thenReturn(false);

        assertThatThrownBy(() -> authService.signup(signupCommand("password")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_PASSWORD_FORMAT);

        verify(passwordHasher, never()).hash(any());
        verify(memberOutPort, never()).save(any());
    }

    @Test
    @DisplayName("소셜 가입은 비밀번호가 비어 있으면 비밀번호 해시 없이 가입할 수 있다")
    void signup_socialUserWithBlankPassword_skipsPasswordValidationAndHashing() {
        when(signupVerificationPort.consumeSignupToken("signup-token"))
                .thenReturn(Optional.of(new SignupVerificationPort.SignupIdentity(
                        "01011112222", LoginType.KAKAO, "kakao-provider-id")));
        when(memberOutPort.existsByPhone("01011112222")).thenReturn(false);
        when(memberOutPort.existsByNickname("회원1")).thenReturn(false);
        when(memberOutPort.save(any())).thenReturn(aMember(10L, null));
        when(tokenIssuer.issue("10", Role.USER))
                .thenReturn(new com.sportsmate.server.common.port.out.token.TokenPair("access", "refresh"));

        AuthUseCase.AuthResult result = authService.signup(signupCommand(" "));

        assertThat(result.accessToken()).isEqualTo("access");
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    @DisplayName("소셜 가입 OTP 검증은 전화번호와 소셜 식별자를 묶은 signupToken을 발급한다")
    void verifySignupCode_withSocialPendingToken_issuesSocialSignupTokenWithPhone() {
        when(signupVerificationPort.verifyCode(SignupVerificationPort.PURPOSE_SIGNUP, "01011112222", "123456"))
                .thenReturn(SignupVerificationPort.CodeStatus.VALID);
        when(memberOutPort.existsByPhone("01011112222")).thenReturn(false);
        when(signupVerificationPort.consumeSocialPendingToken("pending-token"))
                .thenReturn(Optional.of(new SignupVerificationPort.SignupIdentity(
                        null, LoginType.KAKAO, "kakao-provider-id")));
        when(signupVerificationPort.issueSocialSignupToken(
                "01011112222", LoginType.KAKAO, "kakao-provider-id", 1800))
                .thenReturn("signup-token");

        String signupToken = authService.verifySignupCode("01011112222", "123456", "pending-token");

        assertThat(signupToken).isEqualTo("signup-token");
        verify(signupVerificationPort).issueSocialSignupToken(
                "01011112222", LoginType.KAKAO, "kakao-provider-id", 1800);
    }

    @Test
    @DisplayName("회원가입 인증번호 5회 실패 잠금 상태면 전용 예외가 발생한다")
    void verifySignupCode_locked_throwsCodeVerifyLocked() {
        when(signupVerificationPort.verifyCode(SignupVerificationPort.PURPOSE_SIGNUP, "01011112222", "123456"))
                .thenReturn(SignupVerificationPort.CodeStatus.LOCKED);

        assertThatThrownBy(() -> authService.verifySignupCode("01011112222", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.CODE_VERIFY_LOCKED);
    }

    @Test
    @DisplayName("소셜 계정이 다른 회원에게 이미 연동되어 있으면 충돌 예외가 발생한다")
    void linkSocialAccount_providerLinkedToOtherMember_throwsConflict() {
        when(kakaoAuthPort.verify("kakao-token"))
                .thenReturn(new SocialUserInfo("kakao-provider-id", null, null));
        when(memberOutPort.findLinkedMemberId(LoginType.KAKAO, "kakao-provider-id"))
                .thenReturn(Optional.of(99L));

        assertThatThrownBy(() -> authService.linkSocialAccount(1L, "kakao", "kakao-token"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
    }

    @Test
    @DisplayName("마지막 로그인 수단은 해제할 수 없다")
    void unlinkAccount_lastLoginMethod_throwsConflict() {
        when(memberOutPort.hasLoginMethod(1L, LoginType.PHONE)).thenReturn(true);
        when(memberOutPort.countLoginMethods(1L)).thenReturn(1);

        assertThatThrownBy(() -> authService.unlinkAccount(1L, "phone"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.CANNOT_UNLINK_LAST_LOGIN_METHOD);

        verify(memberOutPort, never()).unlinkLoginMethod(any(), any());
    }

    @Test
    @DisplayName("전화번호 미인증 회원이 전화번호 로그인을 연동하면 전용 에러가 발생한다")
    void linkPhoneAccount_phoneNotVerified_throwsPhoneNotVerified() {
        Member member = Member.reconstitute(
                1L, "01012345678", null, null, LoginType.KAKAO, "kakao-provider-id",
                "회원1", "소개", LocalDate.of(1997, 3, 15), Gender.MALE, null,
                "#2E7D32", "LG", List.of(WatchStyle.CHEER), Personality.TENSION,
                TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, false, "서울 송파구 잠실동",
                37.5, 127.0, 0, 0.0, 100, 2, 0, true, Role.USER);
        when(memberOutPort.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> authService.linkPhoneAccount(1L, "password123!"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.PHONE_NOT_VERIFIED);

        verify(passwordHasher, never()).hash(any());
        verify(memberOutPort, never()).linkPhoneAccount(any(), any());
    }

    @Test
    @DisplayName("비밀번호 변경 시 새 비밀번호 형식이 잘못되면 전용 예외가 발생한다")
    void changePassword_withInvalidNewPassword_throwsInvalidPasswordFormat() {
        Member member = aMember(1L, null);
        when(memberOutPort.findById(1L)).thenReturn(Optional.of(member));
        when(passwordHasher.matches("currentPassword123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(1L, "currentPassword123", "password"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_PASSWORD_FORMAT);

        verify(passwordHasher, never()).hash(any());
        verify(memberOutPort, never()).save(any());
    }

    @Test
    @DisplayName("전화번호 로그인 연동 시 비밀번호 형식이 잘못되면 전용 예외가 발생한다")
    void linkPhoneAccount_withInvalidPassword_throwsInvalidPasswordFormat() {
        Member member = Member.reconstitute(
                1L, "01012345678", LocalDateTime.now(), null, LoginType.KAKAO, "kakao-provider-id",
                "회원1", "소개", LocalDate.of(1997, 3, 15), Gender.MALE, null,
                "#2E7D32", "LG", List.of(WatchStyle.CHEER), Personality.TENSION,
                TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, false, "서울 송파구 잠실동",
                37.5, 127.0, 0, 0.0, 100, 2, 0, true, Role.USER);
        when(memberOutPort.findById(1L)).thenReturn(Optional.of(member));
        when(memberOutPort.hasLoginMethod(1L, LoginType.PHONE)).thenReturn(false);

        assertThatThrownBy(() -> authService.linkPhoneAccount(1L, "password"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_PASSWORD_FORMAT);

        verify(passwordHasher, never()).hash(any());
        verify(memberOutPort, never()).linkPhoneAccount(any(), any());
    }

    @Test
    @DisplayName("전화번호 변경 인증에 성공하면 회원 전화번호를 갱신하고 프로필을 반환한다")
    void verifyPhoneChangeCode_success_changesPhone() {
        Member before = aMember(1L, null);
        Member after = Member.reconstitute(
                1L, "01022223333", "encoded-password", LoginType.PHONE, null,
                "회원1", "소개", LocalDate.of(1997, 3, 15), Gender.MALE, null,
                "#2E7D32", "LG", List.of(WatchStyle.CHEER), Personality.TENSION,
                TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, false, "서울 송파구 잠실동",
                37.5, 127.0, 0, 0.0, 100, 2, 0, true, Role.USER);
        when(memberOutPort.findById(1L)).thenReturn(Optional.of(before), Optional.of(after));
        when(phoneChangeLogPort.findLatestByMemberIdSince(eq(1L), any()))
                .thenReturn(Optional.empty());
        when(signupVerificationPort.verifyCode(SignupVerificationPort.PURPOSE_PHONE_CHANGE, "01022223333", "123456"))
                .thenReturn(SignupVerificationPort.CodeStatus.VALID);
        when(memberOutPort.existsByPhone("01022223333")).thenReturn(false);

        var profile = authService.verifyPhoneChangeCode(1L, "01022223333", "123456");

        assertThat(profile.phone()).isEqualTo("01022223333");
        assertThat(profile.phoneVerified()).isTrue();
        verify(memberOutPort).changePhone(1L, "01022223333");
        verify(phoneChangeLogPort).save(argThat(log ->
                log.memberId().equals(1L)
                        && log.oldPhone().equals("01012341")
                        && log.newPhone().equals("01022223333")
                        && log.changedAt() != null));
    }

    @Test
    @DisplayName("전화번호 변경 인증번호 5회 실패 잠금 상태면 전용 예외가 발생한다")
    void verifyPhoneChangeCode_locked_throwsCodeVerifyLocked() {
        Member member = aMember(1L, null);
        when(memberOutPort.findById(1L)).thenReturn(Optional.of(member));
        when(phoneChangeLogPort.findLatestByMemberIdSince(eq(1L), any()))
                .thenReturn(Optional.empty());
        when(signupVerificationPort.verifyCode(SignupVerificationPort.PURPOSE_PHONE_CHANGE, "01022223333", "123456"))
                .thenReturn(SignupVerificationPort.CodeStatus.LOCKED);

        assertThatThrownBy(() -> authService.verifyPhoneChangeCode(1L, "01022223333", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.CODE_VERIFY_LOCKED);

        verify(memberOutPort, never()).changePhone(any(), any());
    }

    @Test
    @DisplayName("최근 30일 내 전화번호 변경 이력이 있으면 인증번호 발송을 차단한다")
    void sendPhoneChangeCode_recentChange_throwsLimitExceeded() {
        Member member = aMember(1L, null);
        LocalDateTime changedAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        when(memberOutPort.findById(1L)).thenReturn(Optional.of(member));
        when(phoneChangeLogPort.findLatestByMemberIdSince(eq(1L), any()))
                .thenReturn(Optional.of(new PhoneChangeLogPort.PhoneChangeLog(
                        1L, "01011112222", "01022223333", changedAt)));

        assertThatThrownBy(() -> authService.sendPhoneChangeCode(1L, "01033334444"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.PHONE_CHANGE_LIMIT_EXCEEDED)
                .extracting("details")
                .satisfies(details -> assertThat(((java.util.Map<?, ?>) details).get("nextAvailableAt"))
                        .isEqualTo("2026-07-31T10:00"));

        verify(signupVerificationPort, never()).checkSendAttempt(any(), any());
        verify(smsSender, never()).send(any(), any());
    }

    @Test
    @DisplayName("최근 30일 내 전화번호 변경 이력이 있으면 인증번호 검증도 차단한다")
    void verifyPhoneChangeCode_recentChange_throwsLimitExceeded() {
        Member member = aMember(1L, null);
        when(memberOutPort.findById(1L)).thenReturn(Optional.of(member));
        when(phoneChangeLogPort.findLatestByMemberIdSince(eq(1L), any()))
                .thenReturn(Optional.of(new PhoneChangeLogPort.PhoneChangeLog(
                        1L, "01011112222", "01022223333", LocalDateTime.now())));

        assertThatThrownBy(() -> authService.verifyPhoneChangeCode(1L, "01033334444", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.PHONE_CHANGE_LIMIT_EXCEEDED);

        verify(signupVerificationPort, never()).verifyCode(any(), any(), any());
        verify(memberOutPort, never()).changePhone(any(), any());
        verify(phoneChangeLogPort, never()).save(any());
    }

    private Member aMember(Long id, String avatarUrl) {
        return Member.reconstitute(
                id, "0101234" + id, "encoded-password", LoginType.PHONE, null,
                "회원" + id, "소개", LocalDate.of(1997, 3, 15), Gender.MALE, avatarUrl,
                "#2E7D32", "LG", List.of(WatchStyle.CHEER), Personality.TENSION,
                TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER,
                GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, false, "서울 송파구 잠실동",
                37.5, 127.0, 0, 0.0, 100, 2, 0, true, Role.USER);
    }

    private AuthUseCase.SignupCommand signupCommand(String password) {
        return new AuthUseCase.SignupCommand(
                "signup-token",
                password,
                "회원1",
                "소개",
                LocalDate.of(1997, 3, 15),
                Gender.MALE,
                "LG",
                List.of(WatchStyle.CHEER),
                Personality.TENSION,
                TalkStyle.TALKATIVE,
                SmokingStatus.NON_SMOKER,
                GenderPref.ANY,
                AgePref.ANY,
                SmokingPref.ANY,
                5,
                true,
                true,
                true,
                true,
                false,
                "서울 송파구 잠실동",
                null,
                null);
    }
}
