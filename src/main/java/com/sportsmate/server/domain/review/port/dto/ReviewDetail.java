package com.sportsmate.server.domain.review.port.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewDetail(
        int rating,
        List<String> tags,
        String comment,
        Boolean profileAccurate,
        List<String> profileMismatchFields,
        LocalDateTime reviewedAt) {
}
