package com.sportsmate.server.domain.policy;

import java.time.LocalDateTime;

public record TermsAgreement(Long termsId, boolean agreed, LocalDateTime agreedAt) {
}
