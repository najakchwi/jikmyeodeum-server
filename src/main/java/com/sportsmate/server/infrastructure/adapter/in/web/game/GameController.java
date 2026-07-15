package com.sportsmate.server.infrastructure.adapter.in.web.game;

import com.sportsmate.server.domain.game.port.in.GameUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Game", description = "경기 일정과 경기 상세 조회 API")
public class GameController {
    private final GameUseCase gameUseCase;

    public GameController(GameUseCase gameUseCase) {
        this.gameUseCase = gameUseCase;
    }

    @GetMapping("/calendar")
    @Operation(
            summary = "경기 캘린더 조회",
            description = "지정한 기간에 경기가 존재하는 날짜 목록을 조회합니다.")
    public ApiResponse<GameUseCase.CalendarResult> calendar(
            @Parameter(description = "조회 시작일", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일", example = "2026-04-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(gameUseCase.calendar(startDate, endDate));
    }

    @GetMapping
    @Operation(
            summary = "경기 목록 조회",
            description = "특정 날짜의 경기 목록을 조회합니다. 로그인 사용자는 본인의 신청 여부가 함께 반영될 수 있습니다.")
    public ApiResponse<List<GameUseCase.GameResult>> games(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "조회할 경기 날짜", example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "필터링할 팀 이름 목록", example = "LG 트윈스")
            @RequestParam(required = false) List<String> teams) {
        return ApiResponse.success(gameUseCase.games(toMemberId(memberId), date, teams));
    }

    @GetMapping("/{gameId}")
    @Operation(summary = "경기 상세 조회", description = "경기 ID로 경기 상세 정보와 신청 가능 상태를 조회합니다.")
    public ApiResponse<GameUseCase.GameResult> game(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "경기 ID", example = "20260401LGKT")
            @PathVariable String gameId) {
        return ApiResponse.success(gameUseCase.game(toMemberId(memberId), gameId));
    }

    private Long toMemberId(String memberId) {
        return memberId == null || "anonymousUser".equals(memberId)
                ? null
                : Long.valueOf(memberId);
    }
}
