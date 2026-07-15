package com.sportsmate.server.infrastructure.adapter.in.web.policy;

import com.sportsmate.server.domain.policy.port.in.PolicyUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/terms")
public class PolicyController {

    private final PolicyUseCase policyUseCase;

    public PolicyController(PolicyUseCase policyUseCase) {
        this.policyUseCase = policyUseCase;
    }

    @GetMapping("/{code}")
    @Operation(summary = "최신 약관 조회", description = "약관 코드에 해당하는 현재 시행 중인 최신 약관을 조회합니다.")
    public ApiResponse<PolicyUseCase.TermsContent> latest(
            @Parameter(description = "약관 코드", example = "service")
            @PathVariable String code) {
        return ApiResponse.success(policyUseCase.getLatestTerm(code));
    }

    @GetMapping("/pending")
    @Operation(summary = "재동의 필요 약관 조회", description = "현재 회원이 아직 동의하지 않았거나 유효기간이 만료된 약관을 조회합니다.")
    public ApiResponse<PolicyUseCase.PendingTermsResult> pending(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        return ApiResponse.success(policyUseCase.getPendingTerms(Long.valueOf(memberId)));
    }

    @PostMapping("/agree")
    @Operation(summary = "약관 동의 저장", description = "약관 코드 기준으로 현재 시행 중인 최신 버전에 대한 동의 이력을 저장합니다.")
    public ResponseEntity<Void> agree(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody AgreeTermsRequest request) {
        policyUseCase.agree(Long.valueOf(memberId), request.toCommands());
        return ResponseEntity.noContent().build();
    }

    public record AgreeTermsRequest(@NotEmpty List<@Valid TermsAgreementRequest> agreements) {
        List<PolicyUseCase.TermsAgreementCommand> toCommands() {
            return agreements.stream()
                    .map(agreement -> new PolicyUseCase.TermsAgreementCommand(agreement.code(), agreement.agreed()))
                    .toList();
        }
    }

    public record TermsAgreementRequest(
            @Schema(description = "약관 코드", example = "service")
            @NotBlank String code,
            @Schema(description = "동의 여부", example = "true")
            boolean agreed
    ) {}
}
