package com.sportsmate.server.domain.content;

import java.time.LocalDateTime;

public record BannerContent(
        String code,
        String title,
        String imageKey,
        String linkUrl,
        int displayOrder,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
