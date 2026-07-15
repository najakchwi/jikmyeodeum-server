package com.sportsmate.server.infrastructure.adapter.in.web.auth;

import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WithdrawalReason;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.in.AuthUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.validation.ValidPassword;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "전화번호 회원가입, 로그인, 소셜 로그인, 토큰 관리 API")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/signup/phone/send-code")
    @Operation(
            summary = "회원가입 인증번호 발송",
            description = "010으로 시작하는 휴대폰 번호로 회원가입용 6자리 인증번호를 발송하고 만료 시간을 반환합니다.")
    public ApiResponse<ExpiresResponse> sendCode(@Valid @RequestBody PhoneRequest request) {
        AuthUseCase.SendCodeResult result = authUseCase.sendSignupCode(request.phone(), request.socialPendingToken());
        return ApiResponse.success(new ExpiresResponse(result.expiresInSeconds(), result.remainingAttempts()));
    }

    @PostMapping("/signup/phone/verify-code")
    @Operation(
            summary = "회원가입 인증번호 검증",
            description = "휴대폰 번호와 인증번호를 검증한 뒤 회원가입 본문에 사용할 일회성 signupToken을 발급합니다.")
    public ApiResponse<SignupTokenResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        return ApiResponse.success(new SignupTokenResponse(
                authUseCase.verifySignupCode(request.phone(), request.code(), request.socialPendingToken())));
    }

    @PostMapping("/signup")
    @Operation(
            summary = "회원가입 완료",
            description = "인증된 signupToken과 프로필, 성향, 선호 조건, 위치 정보를 저장하고 accessToken/refreshToken을 발급합니다.")
    public ApiResponse<AuthUseCase.AuthResult> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.created(authUseCase.signup(request.toCommand()));
    }

    @PostMapping("/login")
    @Operation(summary = "전화번호 로그인", description = "전화번호와 비밀번호로 로그인하고 accessToken/refreshToken을 발급합니다.")
    public ApiResponse<AuthUseCase.AuthResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authUseCase.login(request.phone(), request.password()));
    }

    @PostMapping("/social/{provider}")
    @Operation(
            summary = "소셜 로그인",
            description = "소셜 제공자 토큰을 검증합니다. 기존 회원이면 토큰을 발급하고, 신규 회원이면 추가 가입용 signupToken을 반환합니다.")
    public ApiResponse<AuthUseCase.SocialAuthResult> socialLogin(
            @Parameter(description = "소셜 로그인 제공자", example = "kakao")
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request) {
        return ApiResponse.success(authUseCase.socialLogin(provider, request.providerToken()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "refreshToken을 검증하고 새 accessToken/refreshToken 쌍을 발급합니다.")
    public ApiResponse<AuthUseCase.TokenResult> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authUseCase.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인한 회원의 refreshToken을 폐기합니다.")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        authUseCase.logout(Long.valueOf(memberId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password")
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다.")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authUseCase.changePassword(Long.valueOf(memberId), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset/send-code")
    @Operation(summary = "비밀번호 재설정 인증번호 발송", description = "전화번호 로그인 계정에 비밀번호 재설정용 인증번호를 발송합니다.")
    public ApiResponse<ExpiresResponse> sendResetPasswordCode(@Valid @RequestBody ResetPhoneRequest request) {
        AuthUseCase.SendCodeResult result = authUseCase.sendResetPasswordCode(request.phone());
        return ApiResponse.success(new ExpiresResponse(result.expiresInSeconds(), result.remainingAttempts()));
    }

    @PostMapping("/password/reset/verify-code")
    @Operation(summary = "비밀번호 재설정 인증번호 검증", description = "인증번호를 검증하고 비밀번호 재설정용 일회성 resetToken을 발급합니다.")
    public ApiResponse<ResetTokenResponse> verifyResetPasswordCode(
            @Valid @RequestBody ResetVerifyCodeRequest request) {
        return ApiResponse.success(new ResetTokenResponse(
                authUseCase.verifyResetPasswordCode(request.phone(), request.code())));
    }

    @PatchMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정 완료", description = "resetToken을 소비해 새 비밀번호로 변경하고 기존 refreshToken을 폐기합니다.")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authUseCase.resetPassword(request.resetToken(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/withdraw")
    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 회원을 탈퇴 처리합니다. 개인정보는 즉시 파기되고 refreshToken도 함께 폐기됩니다. "
                    + "신고·제재 이력 등 부정이용 방지를 위한 정보는 별도 보존 정책에 따라 유지됩니다.")
    public ResponseEntity<Void> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody WithdrawRequest request) {
        authUseCase.withdraw(Long.valueOf(memberId), request.reason(), request.reasonDetail());
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "휴대폰 번호 요청")
    public record PhoneRequest(
            @Schema(description = "하이픈 없는 국내 휴대폰 번호", example = "01012345678")
            @NotBlank @Pattern(regexp = "^010\\d{8}$") String phone,
            @Schema(description = "소셜 신규 가입 중 받은 pending token", nullable = true)
            String socialPendingToken) {}
    @Schema(description = "휴대폰 인증번호 검증 요청")
    public record VerifyCodeRequest(
            @Schema(description = "하이픈 없는 국내 휴대폰 번호", example = "01012345678")
            @NotBlank @Pattern(regexp = "^010\\d{8}$") String phone,
            @Schema(description = "SMS로 발송된 6자리 인증번호", example = "123456")
            @NotBlank @Pattern(regexp = "^\\d{6}$") String code,
            @Schema(description = "소셜 신규 가입 중 받은 pending token", nullable = true)
            String socialPendingToken) {}
    @Schema(description = "전화번호 로그인 요청")
    public record LoginRequest(
            @Schema(description = "하이픈 없는 국내 휴대폰 번호", example = "01012345678")
            @NotBlank @Pattern(regexp = "^010\\d{8}$") String phone,
            @Schema(description = "회원 비밀번호", example = "password123!")
            @NotBlank String password) {}
    @Schema(description = "소셜 로그인 요청")
    public record SocialLoginRequest(
            @Schema(description = "소셜 제공자가 발급한 ID 토큰 또는 access token", example = "eyJhbGciOi...")
            @NotBlank String providerToken) {}
    @Schema(description = "토큰 재발급 요청")
    public record RefreshRequest(
            @Schema(description = "로그인 또는 직전 재발급으로 받은 refreshToken", example = "eyJhbGciOi...")
            @NotBlank String refreshToken) {}
    @Schema(description = "비밀번호 변경 요청")
    public record ChangePasswordRequest(
            @Schema(description = "현재 비밀번호", example = "password123!")
            @NotBlank String currentPassword,
            @Schema(description = "새 비밀번호. 영문, 숫자 포함 8자 이상", example = "newPassword123!")
            @NotBlank @ValidPassword String newPassword) {}
    @Schema(description = "비밀번호 재설정 휴대폰 번호 요청")
    public record ResetPhoneRequest(
            @Schema(description = "하이픈 없는 국내 휴대폰 번호", example = "01012345678")
            @NotBlank @Pattern(regexp = "^010\\d{8}$") String phone) {}
    @Schema(description = "비밀번호 재설정 인증번호 검증 요청")
    public record ResetVerifyCodeRequest(
            @Schema(description = "하이픈 없는 국내 휴대폰 번호", example = "01012345678")
            @NotBlank @Pattern(regexp = "^010\\d{8}$") String phone,
            @Schema(description = "SMS로 발송된 6자리 인증번호", example = "123456")
            @NotBlank @Pattern(regexp = "^\\d{6}$") String code) {}
    @Schema(description = "비밀번호 재설정 완료 요청")
    public record ResetPasswordRequest(
            @Schema(description = "비밀번호 재설정 인증번호 검증으로 받은 일회성 토큰", example = "reset_abc123")
            @NotBlank String resetToken,
            @Schema(description = "새 비밀번호. 영문, 숫자 포함 8자 이상", example = "newPassword123!")
            @NotBlank @ValidPassword String newPassword) {}
    @Schema(description = "회원 탈퇴 요청")
    public record WithdrawRequest(
            @Schema(description = "탈퇴 사유", example = "UNSATISFIED_MATCHING")
            @NotNull WithdrawalReason reason,
            @Schema(description = "기타 사유 상세. reason=OTHER일 때 필수", example = "원하는 기능이 부족해요", nullable = true)
            @Size(max = 200) String reasonDetail) {
        @AssertTrue(message = "reasonDetail is required when reason is OTHER")
        public boolean isReasonDetailValid() {
            return reason != WithdrawalReason.OTHER
                    || (reasonDetail != null && !reasonDetail.isBlank());
        }
    }
    @Schema(description = "인증번호 만료 시간 응답")
    public record ExpiresResponse(
            @Schema(description = "인증번호 만료까지 남은 초", example = "180")
            int expiresIn,
            @Schema(description = "현재 발송 후 10분 제한 윈도우 내 남은 발송 가능 횟수", example = "2")
            int remainingAttempts) {}
    @Schema(description = "회원가입 임시 토큰 응답")
    public record SignupTokenResponse(
            @Schema(description = "회원가입 완료 API에 전달할 일회성 토큰", example = "signup_abc123")
            String signupToken) {}
    @Schema(description = "비밀번호 재설정 임시 토큰 응답")
    public record ResetTokenResponse(
            @Schema(description = "비밀번호 재설정 완료 API에 전달할 일회성 토큰", example = "reset_abc123")
            String resetToken) {}

    @Schema(description = "회원가입 완료 요청")
    public record SignupRequest(
            @Schema(description = "인증번호 검증 또는 소셜 신규 가입으로 발급받은 토큰", example = "signup_abc123")
            @NotBlank String signupToken,
            @Schema(description = "비밀번호. 영문, 숫자 포함 8자 이상. 소셜 가입은 null 허용", example = "password123!")
            @ValidPassword String password,
            @Schema(description = "회원 기본 프로필")
            @NotNull ProfileRequest profile,
            @Schema(description = "필수/선택 약관 동의 정보")
            @NotNull AgreementsRequest agreements,
            @Schema(description = "응원팀 이름. 미선택 시 null", example = "LG 트윈스", nullable = true)
            String team,
            @Schema(description = "관람 스타일. 최대 2개", example = "[\"cheer\", \"food\"]")
            @NotEmpty @Size(max = 2) List<WatchStyle> watchStyles,
            @Schema(description = "본인 성격", example = "tension")
            @NotNull Personality personality,
            @Schema(description = "대화 스타일", example = "talkative")
            @NotNull TalkStyle talkStyle,
            @Schema(description = "흡연 여부", example = "non-smoker")
            @NotNull SmokingStatus smokingStatus,
            @Schema(description = "선호 동행 성별", example = "any")
            @NotNull GenderPref genderPref,
            @Schema(description = "선호 동행 연령대", example = "similar")
            @NotNull AgePref agePref,
            @Schema(description = "선호 동행 흡연 여부", example = "non-smoker")
            @NotNull SmokingPref smokingPref,
            @Schema(description = "동행 매칭 허용 거리(km)", example = "10")
            int distanceKm,
            @Schema(description = "활동 위치 정보")
            @NotNull LocationRequest location) {

        AuthUseCase.SignupCommand toCommand() {
            return new AuthUseCase.SignupCommand(
                    signupToken, password, profile.nickname(), profile.bio(), profile.birthdate(),
                    profile.gender(), team, watchStyles,
                    personality, talkStyle, smokingStatus, genderPref, agePref, smokingPref,
                    distanceKm, agreements.service(), agreements.privacy(), agreements.location(),
                    agreements.age14(), agreements.marketing(),
                    location.address(), location.verified() ? location.latitude() : null,
                    location.verified() ? location.longitude() : null);
        }
    }
    @Schema(description = "회원 기본 프로필")
    public record ProfileRequest(
            @Schema(description = "닉네임. 2~12자", example = "야구친구")
            @NotBlank @Size(min = 2, max = 12) String nickname,
            @Schema(description = "한 줄 소개. 최대 50자", example = "잠실 직관 자주 갑니다", nullable = true)
            @Size(max = 50) String bio,
            @Schema(description = "생년월일", example = "1998-04-12")
            @NotNull LocalDate birthdate,
            @Schema(description = "성별", example = "MALE")
            @NotNull Gender gender) {}
    @Schema(description = "약관 동의 정보")
    public record AgreementsRequest(
            @Schema(description = "서비스 이용약관 동의 여부. 필수 true", example = "true")
            @AssertTrue boolean service,
            @Schema(description = "개인정보 처리방침 동의 여부. 필수 true", example = "true")
            @AssertTrue boolean privacy,
            @Schema(description = "위치기반서비스 이용약관 동의 여부. 필수 true", example = "true")
            @AssertTrue boolean location,
            @Schema(description = "만 14세 이상 확인 여부. 필수 true", example = "true")
            @AssertTrue boolean age14,
            @Schema(description = "마케팅 수신 동의 여부", example = "false")
            boolean marketing) {}
    @Schema(description = "활동 위치 요청")
    public record LocationRequest(
            @Schema(description = "위도/경도 검증 완료 여부", example = "true")
            boolean verified,
            @Schema(description = "주소 또는 행정동 표시명", example = "서울특별시 송파구 잠실동")
            String address,
            @Schema(description = "위도. verified가 true일 때 사용", example = "37.5145", nullable = true)
            Double latitude,
            @Schema(description = "경도. verified가 true일 때 사용", example = "127.1059", nullable = true)
            Double longitude) {}
}
