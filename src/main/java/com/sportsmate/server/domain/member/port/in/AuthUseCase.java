package com.sportsmate.server.domain.member.port.in;

import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WithdrawalReason;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

public interface AuthUseCase {
    SendCodeResult sendSignupCode(String phone);
    SendCodeResult sendSignupCode(String phone, String socialPendingToken);
    String verifySignupCode(String phone, String code);
    String verifySignupCode(String phone, String code, String socialPendingToken);
    SendCodeResult sendResetPasswordCode(String phone);
    String verifyResetPasswordCode(String phone, String code);
    void resetPassword(String resetToken, String newPassword);
    AuthResult signup(SignupCommand command);
    AuthResult login(String phone, String password);
    SocialAuthResult socialLogin(String provider, String providerToken);
    TokenResult refresh(String refreshToken);
    void logout(Long memberId);
    void changePassword(Long memberId, String currentPassword, String newPassword);
    void withdraw(Long memberId, WithdrawalReason reason, String reasonDetail);
    LinkedAccountsResult getLinkedAccounts(Long memberId);
    LinkAccountResult linkSocialAccount(Long memberId, String provider, String providerToken);
    LinkAccountResult linkPhoneAccount(Long memberId, String password);
    void unlinkAccount(Long memberId, String loginType);
    SendCodeResult sendPhoneChangeCode(Long memberId, String newPhone);
    MemberProfile verifyPhoneChangeCode(Long memberId, String newPhone, String code);

    record SignupCommand(
            String signupToken, String password, String nickname, String bio, LocalDate birthdate,
            Gender gender, String team, List<WatchStyle> watchStyles, Personality personality,
            TalkStyle talkStyle, SmokingStatus smokingStatus, GenderPref genderPref, AgePref agePref,
            SmokingPref smokingPref, int distanceKm, boolean serviceAgreed, boolean privacyAgreed,
            boolean locationAgreed, boolean age14Agreed, boolean marketingAgreed,
            String locationAddress, Double latitude, Double longitude) {}
    record AuthResult(String accessToken, String refreshToken, MemberProfile user) {}
    record SocialAuthResult(boolean isNewUser, String socialPendingToken, String accessToken,
            String refreshToken, MemberProfile user) {}
    record TokenResult(String accessToken, String refreshToken) {}
    record SendCodeResult(int expiresInSeconds, int remainingAttempts) {}
    record LinkedAccountsResult(List<LinkedAccountItem> accounts) {}
    record LinkedAccountItem(LoginType loginType, boolean linked, LocalDateTime linkedAt) {}
    record LinkAccountResult(boolean created) {}
}
