package com.sportsmate.server.infrastructure.adapter.in.web.game;

import com.sportsmate.server.domain.game.port.in.TeamUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Game", description = "구단 정보 조회 API")
public class TeamController {

    private final TeamUseCase teamUseCase;

    public TeamController(TeamUseCase teamUseCase) {
        this.teamUseCase = teamUseCase;
    }

    @GetMapping
    @Operation(summary = "구단 목록 조회", description = "서비스에서 지원하는 KBO 구단 목록과 엠블럼 URL을 조회합니다.")
    public ApiResponse<List<TeamUseCase.TeamResult>> teams() {
        return ApiResponse.success(teamUseCase.listTeams());
    }
}
