package com.sportsmate.server.infrastructure.adapter.in.web.member;

import com.sportsmate.server.domain.member.port.in.AuthUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import com.sportsmate.server.infrastructure.adapter.in.web.common.validation.ValidPassword;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/me")
@Tag(name = "Member Account", description = "내 로그인 수단 연동과 전화번호 변경 API")
public class MemberAccountController {

    private final AuthUseCase authUseCase;

    public MemberAccountController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @GetMapping("/linked-accounts")
    @Operation(summary = "계정 연동 목록 조회")
    public ApiResponse<AuthUseCase.LinkedAccountsResult> linkedAccounts(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        return ApiResponse.success(authUseCase.getLinkedAccounts(Long.valueOf(memberId)));
    }

    @PostMapping("/linked-accounts/{provider:kakao|google}")
    @Operation(summary = "소셜 계정 연동")
    public ResponseEntity<Void> linkSocialAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable String provider,
            @Valid @RequestBody SocialLinkRequest request) {
        AuthUseCase.LinkAccountResult result = authUseCase.linkSocialAccount(
                Long.valueOf(memberId), provider, request.providerToken());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).build();
    }

    @PostMapping("/linked-accounts/phone")
    @Operation(summary = "전화번호 로그인 연동")
    public ResponseEntity<Void> linkPhoneAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody PhoneLinkRequest request) {
        AuthUseCase.LinkAccountResult result = authUseCase.linkPhoneAccount(Long.valueOf(memberId), request.password());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).build();
    }

    @DeleteMapping("/linked-accounts/{loginType}")
    @Operation(summary = "로그인 수단 연동 해제")
    public ResponseEntity<Void> unlinkAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable String loginType) {
        authUseCase.unlinkAccount(Long.valueOf(memberId), loginType);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/phone/send-code")
    @Operation(summary = "전화번호 변경 인증번호 발송")
    public ApiResponse<ExpiresResponse> sendPhoneChangeCode(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody PhoneChangeSendRequest request) {
        AuthUseCase.SendCodeResult result = authUseCase.sendPhoneChangeCode(
                Long.valueOf(memberId), request.newPhone());
        return ApiResponse.success(new ExpiresResponse(result.expiresInSeconds(), result.remainingAttempts()));
    }

    @PostMapping("/phone/verify-code")
    @Operation(summary = "전화번호 변경 인증번호 검증")
    public ApiResponse<?> verifyPhoneChangeCode(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody PhoneChangeVerifyRequest request) {
        return ApiResponse.success(authUseCase.verifyPhoneChangeCode(
                Long.valueOf(memberId), request.newPhone(), request.code()));
    }

    @Schema(description = "소셜 계정 연동 요청")
    public record SocialLinkRequest(@NotBlank String providerToken) {}

    @Schema(description = "전화번호 로그인 연동 요청")
    public record PhoneLinkRequest(@NotBlank @ValidPassword String password) {}

    @Schema(description = "전화번호 변경 인증번호 발송 요청")
    public record PhoneChangeSendRequest(
            @NotBlank @Pattern(regexp = "^010\\d{8}$") String newPhone) {}

    @Schema(description = "전화번호 변경 인증번호 검증 요청")
    public record PhoneChangeVerifyRequest(
            @NotBlank @Pattern(regexp = "^010\\d{8}$") String newPhone,
            @NotBlank @Pattern(regexp = "^\\d{6}$") String code) {}

    @Schema(description = "인증번호 만료 시간 응답")
    public record ExpiresResponse(int expiresIn, int remainingAttempts) {}
}
