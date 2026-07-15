package com.sportsmate.server.infrastructure.adapter.in.web.admin;

import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditEvent;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.common.port.out.audit.AuditResult;
import com.sportsmate.server.domain.game.port.in.SyncSeasonScheduleUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import com.sportsmate.server.infrastructure.security.authorization.RoleAdminAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/games")
@Tag(name = "Admin", description = "운영자 경기 데이터 관리 API")
@RoleAdminAuth
public class AdminGameSyncController {

    private static final int MAX_SYNC_MONTHS = 24;

    private final SyncSeasonScheduleUseCase syncSeasonScheduleUseCase;
    private final AuditLogPort auditLogPort;

    public AdminGameSyncController(SyncSeasonScheduleUseCase syncSeasonScheduleUseCase, AuditLogPort auditLogPort) {
        this.syncSeasonScheduleUseCase = syncSeasonScheduleUseCase;
        this.auditLogPort = auditLogPort;
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "월별 KBO 경기 일정 동기화",
            description = "지정한 연-월의 KBO 경기 일정 CSV를 외부 소스에서 가져와 생성, 갱신, 취소 상태를 동기화합니다.")
    public ApiResponse<SyncSeasonScheduleUseCase.SyncResult> sync(
            @Parameter(description = "동기화할 연-월. yyyy-MM 형식", example = "2026-06")
            @RequestParam String month,
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        SyncSeasonScheduleUseCase.SyncResult result = syncSeasonScheduleUseCase.sync(parseMonth(month));
        auditLogPort.record(AuditEvent.of(
                AuditCategory.ADMIN_ACTION, "GAME_SCHEDULE_SYNC", "ADMIN", memberId,
                "GAME_SCHEDULE", month, AuditResult.SUCCESS,
                Map.of("created", result.created(), "updated", result.updated(), "cancelled", result.cancelled())));
        return ApiResponse.success(result);
    }

    @PostMapping("/sync/range")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "월 범위 KBO 경기 일정 동기화",
            description = "시작 연-월부터 종료 연-월까지 월별 KBO 경기 일정 CSV를 외부 소스에서 가져와 생성, 갱신, 취소 상태를 동기화합니다.")
    public ApiResponse<SyncSeasonScheduleUseCase.SyncRangeResult> syncRange(
            @RequestBody SyncRangeRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        YearMonth fromMonth = parseMonth(request.fromMonth());
        YearMonth toMonth = parseMonth(request.toMonth());
        validateRange(fromMonth, toMonth);
        SyncSeasonScheduleUseCase.SyncRangeResult result = syncSeasonScheduleUseCase.sync(fromMonth, toMonth);
        auditLogPort.record(AuditEvent.of(
                AuditCategory.ADMIN_ACTION, "GAME_SCHEDULE_SYNC_RANGE", "ADMIN", memberId,
                "GAME_SCHEDULE", request.fromMonth() + "~" + request.toMonth(), AuditResult.SUCCESS,
                Map.of("created", result.created(), "updated", result.updated(), "cancelled", result.cancelled())));
        return ApiResponse.success(result);
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be yyyy-MM", exception);
        }
    }

    private void validateRange(YearMonth fromMonth, YearMonth toMonth) {
        if (fromMonth.isAfter(toMonth)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromMonth must be before or equal to toMonth");
        }
        long monthCount = ChronoUnit.MONTHS.between(fromMonth, toMonth) + 1;
        if (monthCount > MAX_SYNC_MONTHS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month range must be 24 months or less");
        }
    }

    public record SyncRangeRequest(
            @Schema(description = "동기화 시작 연-월. yyyy-MM 형식", example = "2026-06")
            String fromMonth,
            @Schema(description = "동기화 종료 연-월. yyyy-MM 형식", example = "2026-09")
            String toMonth
    ) {
    }
}
