package com.sportsmate.server.domain.member.service;

import com.sportsmate.server.common.exception.AuthErrorCode;
import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditEvent;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.common.port.out.audit.AuditResult;
import com.sportsmate.server.common.port.out.location.KakaoLocalApiPort;
import com.sportsmate.server.common.port.out.monitoring.SafetySignalPort;
import com.sportsmate.server.common.port.out.location.LocationRegion;
import com.sportsmate.server.common.port.out.oauth.GoogleAuthPort;
import com.sportsmate.server.common.port.out.oauth.KakaoAuthPort;
import com.sportsmate.server.common.port.out.oauth.SocialUserInfo;
import com.sportsmate.server.common.port.out.sms.SmsSender;
import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.token.TokenIssuer;
import com.sportsmate.server.common.port.out.token.TokenPair;
import com.sportsmate.server.common.port.out.token.TokenStore;
import com.sportsmate.server.common.vo.PhoneNumber;
import com.sportsmate.server.domain.application.port.in.ApplicationUseCase;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.WithdrawalReason;
import com.sportsmate.server.domain.member.exception.MemberErrorCode;
import com.sportsmate.server.domain.member.policy.PasswordPolicy;
import com.sportsmate.server.domain.member.policy.ProfileOptionPolicy;
import com.sportsmate.server.domain.member.port.in.AuthUseCase;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.member.port.out.MemberWithdrawalLogPort;
import com.sportsmate.server.domain.member.port.out.PasswordHasher;
import com.sportsmate.server.domain.member.port.out.PhoneChangeLogPort;
import com.sportsmate.server.domain.member.port.out.SignupVerificationPort;
import com.sportsmate.server.domain.policy.port.in.PolicyUseCase;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService implements AuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int CODE_EXPIRES_IN = 180;
    private static final int PHONE_CHANGE_LIMIT_DAYS = 30;
    private static final long SIGNUP_TOKEN_EXPIRES_IN_SECONDS = 1800;
    private static final long RESET_TOKEN_EXPIRES_IN_SECONDS = 900;
    private static final long SOCIAL_PENDING_TOKEN_EXPIRES_IN_SECONDS = 600;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MemberOutPort memberOutPort;
    private final MemberWithdrawalLogPort memberWithdrawalLogPort;
    private final PhoneChangeLogPort phoneChangeLogPort;
    private final ApplicationUseCase applicationUseCase;
    private final SignupVerificationPort signupVerificationPort;
    private final SmsSender smsSender;
    private final TokenIssuer tokenIssuer;
    private final TokenStore tokenStore;
    private final PasswordHasher passwordHasher;
    private final KakaoAuthPort kakaoAuthPort;
    private final GoogleAuthPort googleAuthPort;
    private final KakaoLocalApiPort kakaoLocalApi;
    private final PolicyUseCase policyUseCase;
    private final ObjectStorage objectStorage;
    private final AuditLogPort auditLogPort;
    private final SafetySignalPort safetySignalPort;
    private final String verificationCodeOverride;
    private final long refreshTokenExpiration;

    public AuthService(MemberOutPort memberOutPort, MemberWithdrawalLogPort memberWithdrawalLogPort,
            PhoneChangeLogPort phoneChangeLogPort,
            ApplicationUseCase applicationUseCase,
            SignupVerificationPort signupVerificationPort,
            SmsSender smsSender, TokenIssuer tokenIssuer, TokenStore tokenStore,
            PasswordHasher passwordHasher, KakaoAuthPort kakaoAuthPort, GoogleAuthPort googleAuthPort,
            KakaoLocalApiPort kakaoLocalApi,
            PolicyUseCase policyUseCase,
            ObjectStorage objectStorage,
            AuditLogPort auditLogPort,
            SafetySignalPort safetySignalPort,
            @Value("${app.auth.verification-code-override:}") String verificationCodeOverride,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.memberOutPort = memberOutPort;
        this.memberWithdrawalLogPort = memberWithdrawalLogPort;
        this.phoneChangeLogPort = phoneChangeLogPort;
        this.applicationUseCase = applicationUseCase;
        this.signupVerificationPort = signupVerificationPort;
        this.smsSender = smsSender;
        this.tokenIssuer = tokenIssuer;
        this.tokenStore = tokenStore;
        this.passwordHasher = passwordHasher;
        this.kakaoAuthPort = kakaoAuthPort;
        this.googleAuthPort = googleAuthPort;
        this.kakaoLocalApi = kakaoLocalApi;
        this.policyUseCase = policyUseCase;
        this.objectStorage = objectStorage;
        this.auditLogPort = auditLogPort;
        this.safetySignalPort = safetySignalPort;
        this.verificationCodeOverride = verificationCodeOverride;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Override
    public SendCodeResult sendSignupCode(String phone) {
        return sendSignupCode(phone, null);
    }

    @Override
    public SendCodeResult sendSignupCode(String phone, String socialPendingToken) {
        if (socialPendingToken != null && !socialPendingToken.isBlank()) {
            signupVerificationPort.findSocialPendingToken(socialPendingToken)
                    .orElseThrow(() -> new BusinessException(MemberErrorCode.INVALID_SIGNUP_TOKEN));
        }
        if (memberOutPort.existsByPhone(phone)) {
            throw new BusinessException(MemberErrorCode.PHONE_ALREADY_REGISTERED);
        }
        return sendVerificationCode(SignupVerificationPort.PURPOSE_SIGNUP, phone);
    }

    @Override
    public String verifySignupCode(String phone, String code) {
        return verifySignupCode(phone, code, null);
    }

    @Override
    public String verifySignupCode(String phone, String code, String socialPendingToken) {
        verifyCodeOrThrow(SignupVerificationPort.PURPOSE_SIGNUP, phone, code);
        if (memberOutPort.existsByPhone(phone)) {
            throw new BusinessException(MemberErrorCode.PHONE_ALREADY_REGISTERED);
        }
        if (socialPendingToken != null && !socialPendingToken.isBlank()) {
            SignupVerificationPort.SignupIdentity pendingIdentity = signupVerificationPort
                    .consumeSocialPendingToken(socialPendingToken)
                    .orElseThrow(() -> new BusinessException(MemberErrorCode.INVALID_SIGNUP_TOKEN));
            return signupVerificationPort.issueSocialSignupToken(
                    phone, pendingIdentity.loginType(), pendingIdentity.providerId(), SIGNUP_TOKEN_EXPIRES_IN_SECONDS);
        }
        return signupVerificationPort.issuePhoneSignupToken(phone, SIGNUP_TOKEN_EXPIRES_IN_SECONDS);
    }

    @Override
    public SendCodeResult sendResetPasswordCode(String phone) {
        memberOutPort.findByPhone(phone)
                .filter(member -> member.getLoginType() == LoginType.PHONE)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.NO_PHONE_LOGIN_METHOD));
        return sendVerificationCode(SignupVerificationPort.PURPOSE_PASSWORD_RESET, phone);
    }

    @Override
    public String verifyResetPasswordCode(String phone, String code) {
        verifyCodeOrThrow(SignupVerificationPort.PURPOSE_PASSWORD_RESET, phone, code);
        memberOutPort.findByPhone(phone)
                .filter(member -> member.getLoginType() == LoginType.PHONE)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.NO_PHONE_LOGIN_METHOD));
        return signupVerificationPort.issuePasswordResetToken(phone, RESET_TOKEN_EXPIRES_IN_SECONDS);
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        String phone = signupVerificationPort.consumePasswordResetToken(resetToken)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.INVALID_RESET_TOKEN));
        Member member = memberOutPort.findByPhone(phone)
                .filter(found -> found.getLoginType() == LoginType.PHONE)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.NO_PHONE_LOGIN_METHOD));
        validateRequiredPassword(newPassword);
        if (passwordHasher.matches(newPassword, member.getPassword())) {
            throw new BusinessException(MemberErrorCode.SAME_AS_CURRENT_PASSWORD);
        }
        member.changePassword(passwordHasher.hash(newPassword));
        memberOutPort.save(member);
        tokenStore.deleteByMemberId(member.getId().toString());
    }

    @Override
    @Transactional
    public AuthResult signup(SignupCommand command) {
        SignupVerificationPort.SignupIdentity identity = signupVerificationPort
                .consumeSignupToken(command.signupToken())
                .orElseThrow(() -> new BusinessException(MemberErrorCode.INVALID_SIGNUP_TOKEN));
        if (!command.serviceAgreed() || !command.privacyAgreed()
                || !command.locationAgreed() || !command.age14Agreed()) {
            throw new BusinessException(com.sportsmate.server.common.exception.CommonErrorCode.INVALID_INPUT);
        }
        if (identity.phone() != null && memberOutPort.existsByPhone(identity.phone())) {
            throw new BusinessException(MemberErrorCode.PHONE_ALREADY_REGISTERED);
        }
        if (memberOutPort.existsByNickname(command.nickname())) {
            throw new BusinessException(MemberErrorCode.NICKNAME_ALREADY_USED);
        }
        validatePasswordIfPresent(command.password());
        ProfileOptionPolicy.validateStyle(command.watchStyles(), command.personality(),
                command.talkStyle(), command.smokingStatus());
        String encodedPassword = PasswordPolicy.isBlank(command.password())
                ? null : passwordHasher.hash(command.password());
        String locationAddress = resolveLocationAddress(command);
        Member member = Member.create(
                identity.phone(), encodedPassword, identity.loginType(), identity.providerId(),
                command.nickname(), command.bio(), command.birthdate(), command.gender(),
                command.team(), command.watchStyles(), command.personality(), command.talkStyle(),
                command.smokingStatus(), command.genderPref(), command.agePref(),
                command.smokingPref(), command.distanceKm(), locationAddress,
                command.latitude(), command.longitude(), command.marketingAgreed());
        Member saved = memberOutPort.save(member);
        policyUseCase.recordSignupAgreementsForMember(saved.getId(), new PolicyUseCase.SignupAgreements(
                command.serviceAgreed(),
                command.privacyAgreed(),
                command.locationAgreed(),
                command.age14Agreed(),
                command.marketingAgreed()));
        return authenticated(saved);
    }

    private String resolveLocationAddress(SignupCommand command) {
        if (command.latitude() == null || command.longitude() == null) {
            return null;
        }
        LocationRegion region = kakaoLocalApi.reverseGeocode(command.latitude(), command.longitude());
        return region.toAddress();
    }

    @Override
    public AuthResult login(String phone, String password) {
        Optional<Member> found = memberOutPort.findByPhone(phone)
                .filter(member -> member.getLoginType() == LoginType.PHONE)
                .filter(member -> member.getPassword() != null && passwordHasher.matches(password, member.getPassword()));

        if (found.isEmpty()) {
            auditLogPort.record(AuditEvent.of(
                    AuditCategory.AUTH_LOGIN, "LOGIN_FAILED", "MEMBER", null,
                    "MEMBER", null, AuditResult.FAILURE, Map.of("phone", maskPhone(phone))));
            throw new BusinessException(MemberErrorCode.INVALID_CREDENTIALS);
        }

        Member member = found.get();
        auditLogPort.record(AuditEvent.of(
                AuditCategory.AUTH_LOGIN, "LOGIN_SUCCESS", "MEMBER", member.getId().toString(),
                "MEMBER", member.getId().toString(), AuditResult.SUCCESS, Map.of()));
        return authenticated(member);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    @Override
    public SocialAuthResult socialLogin(String provider, String providerToken) {
        LoginType loginType;
        SocialUserInfo socialUser;
        try {
            loginType = LoginType.valueOf(provider.toUpperCase(Locale.ROOT));
            socialUser = switch (loginType) {
                case KAKAO -> kakaoAuthPort.verify(providerToken);
                case GOOGLE -> googleAuthPort.verify(providerToken);
                default -> throw new IllegalArgumentException();
            };
        } catch (RuntimeException exception) {
            throw new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
        return memberOutPort.findByProvider(loginType, socialUser.providerId())
                .map(member -> {
                    AuthResult auth = authenticated(member);
                    return new SocialAuthResult(false, null, auth.accessToken(),
                            auth.refreshToken(), auth.user());
                })
                .orElseGet(() -> new SocialAuthResult(
                        true,
                        signupVerificationPort.issueSocialPendingToken(
                                loginType, socialUser.providerId(), SOCIAL_PENDING_TOKEN_EXPIRES_IN_SECONDS),
                        null, null, null));
    }

    @Override
    public TokenResult refresh(String refreshToken) {
        String memberId = tokenStore.findMemberIdByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        Member member = memberOutPort.findById(Long.valueOf(memberId))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        TokenPair tokens = tokenIssuer.issue(memberId, member.getRole());
        tokenStore.save(memberId, tokens.refreshToken(), refreshTokenExpiration);
        return new TokenResult(tokens.accessToken(), tokens.refreshToken());
    }

    @Override
    public void logout(Long memberId) {
        tokenStore.deleteByMemberId(memberId.toString());
    }

    @Override
    @Transactional
    public void changePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        if (member.getLoginType() != LoginType.PHONE) {
            throw new BusinessException(MemberErrorCode.PHONE_LOGIN_ONLY);
        }
        if (!passwordHasher.matches(currentPassword, member.getPassword())) {
            throw new BusinessException(MemberErrorCode.INVALID_CURRENT_PASSWORD);
        }
        validateRequiredPassword(newPassword);
        if (passwordHasher.matches(newPassword, member.getPassword())) {
            throw new BusinessException(MemberErrorCode.SAME_AS_CURRENT_PASSWORD);
        }
        member.changePassword(passwordHasher.hash(newPassword));
        memberOutPort.save(member);
    }

    @Override
    @Transactional
    public void withdraw(Long memberId, WithdrawalReason reason, String reasonDetail) {
        Member member = memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        memberWithdrawalLogPort.save(new MemberWithdrawalLogPort.WithdrawalLog(
                memberId,
                member.getPhone(),
                member.getNickname(),
                reason,
                reasonDetail,
                LocalDateTime.now()));
        applicationUseCase.cancelAllActiveByMember(memberId);
        memberOutPort.withdraw(memberId);
        recordWithdrawalSignal();
        tokenStore.deleteByMemberId(memberId.toString());
        deleteAvatarIfPresent(member.getAvatarUrl());
    }

    private void recordWithdrawalSignal() {
        try {
            safetySignalPort.recordWithdrawal();
        } catch (RuntimeException exception) {
            log.warn("Failed to record withdrawal monitoring signal.", exception);
        }
    }

    @Override
    public LinkedAccountsResult getLinkedAccounts(Long memberId) {
        memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        return new LinkedAccountsResult(memberOutPort.findLinkedAccounts(memberId).stream()
                .map(account -> new LinkedAccountItem(account.loginType(), account.linked(), account.linkedAt()))
                .toList());
    }

    @Override
    @Transactional
    public LinkAccountResult linkSocialAccount(Long memberId, String provider, String providerToken) {
        LoginType loginType = parseSocialLoginType(provider);
        SocialUserInfo socialUser = verifySocialToken(loginType, providerToken);
        Optional<Long> linkedMemberId = memberOutPort.findLinkedMemberId(loginType, socialUser.providerId());
        if (linkedMemberId.isPresent()) {
            if (linkedMemberId.get().equals(memberId)) {
                return new LinkAccountResult(false);
            }
            throw new BusinessException(MemberErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }
        memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        memberOutPort.linkSocialAccount(memberId, loginType, socialUser.providerId());
        return new LinkAccountResult(true);
    }

    @Override
    @Transactional
    public LinkAccountResult linkPhoneAccount(Long memberId, String password) {
        Member member = memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        if (member.getPhone() == null || member.getPhone().isBlank() || !member.isPhoneVerified()) {
            throw new BusinessException(MemberErrorCode.PHONE_NOT_VERIFIED);
        }
        if (memberOutPort.hasLoginMethod(memberId, LoginType.PHONE)) {
            return new LinkAccountResult(false);
        }
        validateRequiredPassword(password);
        memberOutPort.linkPhoneAccount(memberId, passwordHasher.hash(password));
        return new LinkAccountResult(true);
    }

    private void validatePasswordIfPresent(String password) {
        if (!PasswordPolicy.isBlank(password) && !PasswordPolicy.isValid(password)) {
            throw new BusinessException(MemberErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }

    private void validateRequiredPassword(String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new BusinessException(MemberErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }

    @Override
    @Transactional
    public void unlinkAccount(Long memberId, String loginTypeValue) {
        LoginType loginType = parseLoginType(loginTypeValue);
        if (!memberOutPort.hasLoginMethod(memberId, loginType)) {
            throw new BusinessException(MemberErrorCode.LOGIN_METHOD_NOT_FOUND);
        }
        if (memberOutPort.countLoginMethods(memberId) <= 1) {
            throw new BusinessException(MemberErrorCode.CANNOT_UNLINK_LAST_LOGIN_METHOD);
        }
        memberOutPort.unlinkLoginMethod(memberId, loginType);
    }

    @Override
    public SendCodeResult sendPhoneChangeCode(Long memberId, String newPhone) {
        memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        assertPhoneChangeAllowed(memberId);
        if (memberOutPort.existsByPhone(newPhone)) {
            throw new BusinessException(MemberErrorCode.PHONE_ALREADY_REGISTERED);
        }
        return sendVerificationCode(SignupVerificationPort.PURPOSE_PHONE_CHANGE, newPhone);
    }

    @Override
    @Transactional
    public MemberProfile verifyPhoneChangeCode(Long memberId, String newPhone, String code) {
        Member member = memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        assertPhoneChangeAllowed(memberId);
        verifyCodeOrThrow(SignupVerificationPort.PURPOSE_PHONE_CHANGE, newPhone, code);
        if (memberOutPort.existsByPhone(newPhone)) {
            throw new BusinessException(MemberErrorCode.PHONE_ALREADY_REGISTERED);
        }
        LocalDateTime changedAt = LocalDateTime.now();
        memberOutPort.changePhone(memberId, newPhone);
        phoneChangeLogPort.save(new PhoneChangeLogPort.PhoneChangeLog(
                memberId,
                member.getPhone(),
                newPhone,
                changedAt));
        return MemberProfile.from(memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND)));
    }

    private void assertPhoneChangeAllowed(Long memberId) {
        phoneChangeLogPort.findLatestByMemberIdSince(
                        memberId,
                        LocalDateTime.now().minusDays(PHONE_CHANGE_LIMIT_DAYS))
                .ifPresent(log -> {
                    LocalDateTime nextAvailableAt = log.changedAt().plusDays(PHONE_CHANGE_LIMIT_DAYS);
                    throw new BusinessException(MemberErrorCode.PHONE_CHANGE_LIMIT_EXCEEDED,
                            Map.of("nextAvailableAt", nextAvailableAt.toString()));
                });
    }

    private void deleteAvatarIfPresent(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return;
        String key = objectStorage.extractKey(avatarUrl);
        if (key == null || key.isBlank()) return;
        try {
            objectStorage.delete(key);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete avatar on withdraw: {}", key, exception);
        }
    }

    private SendCodeResult sendVerificationCode(String purpose, String phone) {
        SignupVerificationPort.SendAttemptStatus sendAttemptStatus = signupVerificationPort
                .checkSendAttempt(purpose, phone);
        if (!sendAttemptStatus.allowed()) {
            throw new BusinessException(MemberErrorCode.SMS_SEND_LIMIT_EXCEEDED,
                    Map.of(
                            "retryAfterSeconds", sendAttemptStatus.retryAfterSeconds(),
                            "reason", sendAttemptStatus.reason().name()));
        }
        String code = verificationCode();
        try {
            smsSender.send(new PhoneNumber(phone), "[직며듦] 인증번호는 " + code + "입니다.");
        } catch (RuntimeException exception) {
            throw new BusinessException(MemberErrorCode.SMS_SEND_FAILED);
        }
        signupVerificationPort.saveCode(purpose, phone, code, CODE_EXPIRES_IN);
        signupVerificationPort.recordSendAttempt(purpose, phone);
        return new SendCodeResult(CODE_EXPIRES_IN, Math.max(0, sendAttemptStatus.remainingAttempts() - 1));
    }

    private void verifyCodeOrThrow(String purpose, String phone, String code) {
        SignupVerificationPort.CodeStatus status = signupVerificationPort.verifyCode(purpose, phone, code);
        if (status == SignupVerificationPort.CodeStatus.EXPIRED) {
            throw new BusinessException(MemberErrorCode.CODE_EXPIRED);
        }
        if (status == SignupVerificationPort.CodeStatus.LOCKED) {
            throw new BusinessException(MemberErrorCode.CODE_VERIFY_LOCKED);
        }
        if (status != SignupVerificationPort.CodeStatus.VALID) {
            throw new BusinessException(MemberErrorCode.INVALID_CODE);
        }
    }

    private AuthResult authenticated(Member member) {
        TokenPair tokens = tokenIssuer.issue(member.getId().toString(), member.getRole());
        tokenStore.save(member.getId().toString(), tokens.refreshToken(), refreshTokenExpiration);
        return new AuthResult(tokens.accessToken(), tokens.refreshToken(), MemberProfile.from(member));
    }

    private LoginType parseSocialLoginType(String provider) {
        LoginType loginType = parseLoginType(provider);
        if (loginType == LoginType.PHONE) {
            throw new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
        return loginType;
    }

    private LoginType parseLoginType(String value) {
        try {
            return LoginType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new BusinessException(com.sportsmate.server.common.exception.CommonErrorCode.INVALID_INPUT);
        }
    }

    private SocialUserInfo verifySocialToken(LoginType loginType, String providerToken) {
        try {
            return switch (loginType) {
                case KAKAO -> kakaoAuthPort.verify(providerToken);
                case GOOGLE -> googleAuthPort.verify(providerToken);
                default -> throw new IllegalArgumentException();
            };
        } catch (RuntimeException exception) {
            throw new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    private String verificationCode() {
        if (verificationCodeOverride != null && !verificationCodeOverride.isBlank()) {
            return verificationCodeOverride;
        }
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
