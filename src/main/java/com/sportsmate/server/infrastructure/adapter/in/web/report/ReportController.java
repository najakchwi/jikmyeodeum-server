package com.sportsmate.server.infrastructure.adapter.in.web.report;

import com.sportsmate.server.domain.report.port.in.ReportUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Report", description = "사용자 신고 API")
public class ReportController {
    private static final Set<String> REASONS = Set.of(
            "욕설 및 비하 발언", "스팸 또는 홍보", "성희롱 및 부적절한 언행",
            "사기 또는 허위 정보", "개인정보 요구", "기타",
            "연락두절/노쇼", "욕설·비매너", "사기·허위정보");
    private final ReportUseCase reportUseCase;

    public ReportController(ReportUseCase reportUseCase) {
        this.reportUseCase = reportUseCase;
    }
    @PostMapping
    @Operation(
            summary = "사용자 신고 접수",
            description = "채팅 또는 신청 맥락에서 상대 사용자를 신고합니다. reason은 서버가 허용하는 신고 사유 중 하나여야 합니다.")
    public ApiResponse<ReportUseCase.ReportResult> report(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody ReportRequest request) {
        if (!REASONS.contains(request.reason())) {
            throw new com.sportsmate.server.common.exception.BusinessException(
                    com.sportsmate.server.common.exception.CommonErrorCode.INVALID_INPUT);
        }
        return ApiResponse.created(reportUseCase.report(
                Long.valueOf(memberId), request.targetUserId(), request.applicationId(),
                request.chatId(), request.reason(), request.detail()));
    }
    @Schema(description = "사용자 신고 요청")
    public record ReportRequest(
            @Schema(description = "신고 대상 회원 ID", example = "2")
            @NotNull Long targetUserId,
            @Schema(description = "신고가 발생한 신청 ID", example = "app_123")
            @NotBlank String applicationId,
            @Schema(description = "신고가 발생한 채팅방 ID", example = "chat_123")
            @NotBlank String chatId,
            @Schema(
                    description = "신고 사유. 욕설 및 비하 발언, 스팸 또는 홍보, 성희롱 및 부적절한 언행, 사기 또는 허위 정보, 개인정보 요구, 기타 등",
                    example = "욕설 및 비하 발언")
            @NotBlank String reason,
            @Schema(description = "상세 신고 내용. 최대 1000자", example = "채팅에서 반복적으로 욕설을 했습니다.", nullable = true)
            @Size(max = 1000) String detail) {}
}
