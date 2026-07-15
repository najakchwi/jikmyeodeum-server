package com.sportsmate.server.infrastructure.adapter.in.web.review;

import com.sportsmate.server.domain.review.port.in.ReviewUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Review", description = "동행 평가 API")
public class ReviewController {
    private final ReviewUseCase reviewUseCase;

    public ReviewController(ReviewUseCase reviewUseCase) {
        this.reviewUseCase = reviewUseCase;
    }
    @PostMapping("/{applicationId}/review")
    @Operation(summary = "동행 평가 작성", description = "완료된 동행 신청에 대해 평점, 태그, 코멘트를 등록합니다.")
    public ApiResponse<ReviewUseCase.ReviewResult> review(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "평가할 신청 ID", example = "app_123")
            @PathVariable String applicationId,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.created(reviewUseCase.review(
                Long.valueOf(memberId), applicationId, request.rating(),
                request.tags() == null ? List.of() : request.tags(), request.comment(),
                request.profileAccurate(),
                request.profileMismatchFields() == null ? List.of() : request.profileMismatchFields()));
    }
    @Schema(description = "동행 평가 작성 요청")
    public record ReviewRequest(
            @Schema(description = "평점. 1~5점", example = "5")
            @Min(1) @Max(5) int rating,
            @Schema(description = "평가 태그 목록", example = "[\"시간을 잘 지켜요\", \"응원이 즐거워요\"]")
            List<String> tags,
            @Schema(description = "평가 코멘트. 최대 1000자", example = "같이 응원하기 편하고 매너가 좋았어요.", nullable = true)
            @Size(max = 1000) String comment,
            @Schema(description = "상대 프로필이 실제와 일치했는지 여부", example = "false", nullable = true)
            Boolean profileAccurate,
            @Schema(description = "프로필 불일치 항목. profileAccurate=false일 때만 저장", example = "[\"smoking\", \"watch_style\"]")
            List<String> profileMismatchFields) {}
}
