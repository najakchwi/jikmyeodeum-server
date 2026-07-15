package com.sportsmate.server.infrastructure.adapter.in.web.application;

import com.sportsmate.server.domain.application.port.in.ApplicationUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Application", description = "경기 동행 신청과 매칭 상태 관리 API")
public class ApplicationController {
    private final ApplicationUseCase applicationUseCase;

    public ApplicationController(ApplicationUseCase applicationUseCase) {
        this.applicationUseCase = applicationUseCase;
    }

    @PostMapping("/games/{gameId}/applications")
    @Operation(
            summary = "경기 동행 신청",
            description = "선택한 경기에 현재 로그인한 회원의 동행 신청을 생성합니다.")
    public ApiResponse<ApplicationUseCase.ApplicationResult> apply(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "신청할 경기 ID", example = "20260401LGKT")
            @PathVariable String gameId) {
        return ApiResponse.created(applicationUseCase.apply(Long.valueOf(memberId), gameId));
    }

    @GetMapping("/applications")
    @Operation(
            summary = "내 신청 목록 조회",
            description = "현재 로그인한 회원의 동행 신청 목록을 날짜와 상태 조건으로 필터링해 조회합니다.")
    public ApiResponse<List<ApplicationUseCase.ApplicationResult>> applications(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "조회할 경기 날짜", example = "2026-04-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "신청 상태 목록. 예: PENDING, MATCHED, CANCELLED", example = "PENDING")
            @RequestParam(required = false) List<String> status) {
        return ApiResponse.success(applicationUseCase.applications(Long.valueOf(memberId), date, status));
    }

    @GetMapping("/applications/calendar")
    @Operation(summary = "내 신청 캘린더 조회", description = "특정 연월에 내가 신청한 경기가 있는 날짜 목록을 반환합니다.")
    public ApiResponse<CalendarResponse> calendar(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "조회 연도", example = "2026") @RequestParam int year,
            @Parameter(description = "조회 월(1~12)", example = "4") @RequestParam int month) {
        return ApiResponse.success(new CalendarResponse(
                applicationUseCase.calendar(Long.valueOf(memberId), year, month)));
    }

    @GetMapping("/applications/{applicationId}")
    @Operation(summary = "신청 상세 조회", description = "내 동행 신청 상세와 매칭 관련 정보를 조회합니다.")
    public ApiResponse<ApplicationUseCase.ApplicationResult> get(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "신청 ID", example = "app_123")
            @PathVariable String applicationId) {
        return ApiResponse.success(applicationUseCase.get(Long.valueOf(memberId), applicationId));
    }

    @DeleteMapping("/applications/{applicationId}")
    @Operation(summary = "신청 취소", description = "내 동행 신청을 취소합니다. 이미 확정된 상태에서는 정책에 따라 실패할 수 있습니다.")
    public ResponseEntity<Void> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "취소할 신청 ID", example = "app_123")
            @PathVariable String applicationId) {
        applicationUseCase.cancel(Long.valueOf(memberId), applicationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/applications/{applicationId}/status")
    @Operation(summary = "매칭 상태 조회", description = "신청의 현재 매칭 상태, 매칭 상대 프로필, 채팅방 정보를 조회합니다.")
    public ApiResponse<ApplicationUseCase.MatchStatusResult> status(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "신청 ID", example = "app_123")
            @PathVariable String applicationId) {
        return ApiResponse.success(applicationUseCase.status(Long.valueOf(memberId), applicationId));
    }

    @PostMapping("/applications/{applicationId}/accept")
    @Operation(summary = "매칭 수락", description = "제안된 동행 매칭을 수락하고 신청 상태를 갱신합니다.")
    public ApiResponse<ApplicationUseCase.ApplicationResult> accept(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "신청 ID", example = "app_123")
            @PathVariable String applicationId) {
        return ApiResponse.success(applicationUseCase.accept(Long.valueOf(memberId), applicationId));
    }

    @PostMapping("/applications/{applicationId}/reject")
    @Operation(summary = "매칭 거절", description = "제안된 동행 매칭을 거절하고 다음 매칭 후보 탐색 대상으로 돌립니다.")
    public ApiResponse<ApplicationUseCase.ApplicationResult> reject(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "신청 ID", example = "app_123")
            @PathVariable String applicationId) {
        return ApiResponse.success(applicationUseCase.reject(Long.valueOf(memberId), applicationId));
    }

    @Schema(description = "신청이 존재하는 캘린더 날짜 응답")
    public record CalendarResponse(
            @Schema(description = "해당 연월에서 신청 내역이 있는 날짜 목록", example = "[\"2026-04-01\", \"2026-04-03\"]")
            List<LocalDate> dates) {}
}
