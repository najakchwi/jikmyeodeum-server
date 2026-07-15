package com.sportsmate.server.infrastructure.adapter.in.web.admin;

import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditEvent;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.common.port.out.audit.AuditResult;
import com.sportsmate.server.domain.application.port.in.ApplicationUseCase;
import com.sportsmate.server.domain.application.port.in.ApplicationUseCase.MatchBatchResult;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import com.sportsmate.server.infrastructure.security.authorization.RoleAdminAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/matching")
@Tag(name = "Admin", description = "운영자 매칭 테스트 API")
@RoleAdminAuth
public class AdminMatchingController {

    private final ApplicationUseCase applicationUseCase;
    private final AuditLogPort auditLogPort;

    public AdminMatchingController(ApplicationUseCase applicationUseCase, AuditLogPort auditLogPort) {
        this.applicationUseCase = applicationUseCase;
        this.auditLogPort = auditLogPort;
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "대기 중인 매칭 수동 실행",
            description = "매일 오전 9시 스케줄러(MatchingScheduler)가 실행하는 것과 동일한 매칭 배치를 "
                    + "즉시 실행한다. waiting 상태 신청을 실제로 매칭시켜 matched로 전환한다(드라이런 아님).")
    public ApiResponse<MatchBatchResult> run(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        MatchBatchResult result = applicationUseCase.matchWaitingApplications();
        auditLogPort.record(AuditEvent.of(
                AuditCategory.ADMIN_ACTION, "MANUAL_MATCHING_RUN", "ADMIN", memberId,
                "MATCHING_BATCH", null, AuditResult.SUCCESS,
                Map.of(
                        "gamesProcessed", result.gamesProcessed(),
                        "gamesFailed", result.gamesFailed(),
                        "pairsMatched", result.pairsMatched())));
        return ApiResponse.success(result);
    }
}
