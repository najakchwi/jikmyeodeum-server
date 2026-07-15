package com.sportsmate.server.infrastructure.adapter.in.web.review;

import com.sportsmate.server.domain.review.port.in.ReviewQueryUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Review", description = "동행 평가 API")
public class MemberReviewController {
    private final ReviewQueryUseCase reviewQueryUseCase;

    public MemberReviewController(ReviewQueryUseCase reviewQueryUseCase) {
        this.reviewQueryUseCase = reviewQueryUseCase;
    }

    @GetMapping("/me/reviews")
    @Operation(summary = "내가 받은 리뷰 조회", description = "로그인 회원이 받은 리뷰 요약, 별점 분포, 태그 집계, 코멘트 목록을 조회합니다.")
    public ApiResponse<ReviewQueryUseCase.ReceivedReviewsResult> receivedReviews(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "다음 페이지 조회 커서", example = "rv_61")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "코멘트 페이지 크기. 기본 20, 최대 50", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reviewQueryUseCase.getReceivedReviews(
                Long.valueOf(memberId), cursor, size));
    }

    @GetMapping("/{memberId}/reviews")
    @Operation(summary = "회원이 받은 리뷰 조회", description = "지정한 회원이 받은 리뷰 요약, 별점 분포, 태그 집계, 코멘트 목록을 조회합니다.")
    public ApiResponse<ReviewQueryUseCase.ReceivedReviewsResult> receivedReviewsByMember(
            @Parameter(description = "회원 ID", example = "8")
            @PathVariable Long memberId,
            @Parameter(description = "다음 페이지 조회 커서", example = "rv_61")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "코멘트 페이지 크기. 기본 20, 최대 50", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reviewQueryUseCase.getReceivedReviews(memberId, cursor, size));
    }
}
