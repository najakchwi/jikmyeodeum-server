package com.sportsmate.server.infrastructure.adapter.in.web.staticcontent;

import com.sportsmate.server.domain.content.port.in.ContentUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Static Content", description = "앱 화면에 노출되는 정적 콘텐츠 API")
public class StaticContentController {

    private final ContentUseCase contentUseCase;

    public StaticContentController(ContentUseCase contentUseCase) {
        this.contentUseCase = contentUseCase;
    }

    @GetMapping("/banners")
    @Operation(summary = "홈 배너 목록 조회", description = "홈 화면 등에 노출할 배너 이미지와 링크 정보를 조회합니다.")
    public ApiResponse<BannersResponse> banners() {
        List<Banner> banners = contentUseCase.getBootstrapContent().banners().stream()
                .map(banner -> new Banner(banner.code(), banner.imageUrl(), banner.linkUrl()))
                .toList();
        return ApiResponse.success(new BannersResponse(banners));
    }

    @GetMapping("/faqs")
    @Operation(summary = "FAQ 목록 조회", description = "자주 묻는 질문과 답변 목록을 조회합니다.")
    public ApiResponse<FaqsResponse> faqs() {
        List<Faq> faqs = contentUseCase.getBootstrapContent().faqs().stream()
                .map(faq -> new Faq(faq.code(), faq.question(), faq.answer()))
                .toList();
        return ApiResponse.success(new FaqsResponse(faqs));
    }

    @Schema(description = "배너")
    public record Banner(
            @Schema(description = "배너 ID", example = "b1")
            String id,
            @Schema(description = "배너 이미지 URL", example = "https://cdn.example.com/images/banners/home-main-1.png")
            String imageUrl,
            @Schema(description = "클릭 시 이동할 URL. 없으면 null", example = "https://example.com/event", nullable = true)
            String linkUrl) {}
    @Schema(description = "배너 목록 응답")
    public record BannersResponse(
            @Schema(description = "배너 목록")
            List<Banner> banners) {}
    @Schema(description = "FAQ")
    public record Faq(
            @Schema(description = "FAQ ID", example = "f1")
            String id,
            @Schema(description = "질문", example = "매칭은 어떻게 진행되나요?")
            String question,
            @Schema(description = "답변", example = "같은 경기에 신청한 사용자 중 성향과 거리를 고려해 매칭합니다.")
            String answer) {}
    @Schema(description = "FAQ 목록 응답")
    public record FaqsResponse(
            @Schema(description = "FAQ 목록")
            List<Faq> faqs) {}
}
