package com.sportsmate.server.domain.policy;

import java.time.LocalDateTime;

public record Terms(
        Long id,
        String code,
        String version,
        String title,
        String content,
        String contentKey,
        boolean required,
        Integer validDays,
        LocalDateTime effectiveAt
) {
    public boolean isAgreementValid(LocalDateTime agreedAt, LocalDateTime now) {
        if (agreedAt == null) {
            return false;
        }
        if (validDays == null) {
            return true;
        }
        return agreedAt.plusDays(validDays).isAfter(now);
    }
}
