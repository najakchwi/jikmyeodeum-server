package com.sportsmate.server.infrastructure.adapter.in.web.content;

import com.sportsmate.server.domain.content.port.in.ContentUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    private final ContentUseCase contentUseCase;

    public ContentController(ContentUseCase contentUseCase) {
        this.contentUseCase = contentUseCase;
    }

    @GetMapping("/bootstrap")
    @Operation(summary = "앱 콘텐츠 부트스트랩 조회", description = "배너, FAQ, 아바타 프리셋, 공용 에셋, 구단 정보를 한 번에 조회합니다.")
    public ApiResponse<ContentUseCase.BootstrapContent> bootstrap() {
        return ApiResponse.success(contentUseCase.getBootstrapContent());
    }
}
