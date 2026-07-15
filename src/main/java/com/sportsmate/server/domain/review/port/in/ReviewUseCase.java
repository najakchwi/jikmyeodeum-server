package com.sportsmate.server.domain.review.port.in;

import java.util.List;

public interface ReviewUseCase {
    ReviewResult review(Long memberId, String applicationId, int rating,
            List<String> tags, String comment, Boolean profileAccurate,
            List<String> profileMismatchFields);
    record ReviewResult(String applicationId, String status) {}
}
