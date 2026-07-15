package com.sportsmate.server.domain.policy.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface PolicyUseCase {

    TermsContent getLatestTerm(String code);

    PendingTermsResult getPendingTerms(Long memberId);

    void agree(Long memberId, List<TermsAgreementCommand> agreements);

    void recordSignupAgreementsForMember(Long memberId, SignupAgreements agreements);

    void recordSignupAgreements(Long authId, SignupAgreements agreements);

    record TermsContent(
            String code,
            String version,
            String title,
            String content,
            String contentKey,
            boolean required,
            Integer validDays,
            LocalDateTime effectiveAt
    ) {}

    record PendingTerm(
            Long termsId,
            String code,
            String version,
            String title,
            String content,
            String contentKey,
            PendingTermReason reason,
            boolean required,
            boolean blocking,
            Integer validDays,
            LocalDateTime effectiveAt
    ) {}

    record PendingTermsResult(boolean hasBlockingRequiredTerms, List<PendingTerm> terms) {}

    enum PendingTermReason {
        NEW_VERSION,
        EXPIRED
    }

    record TermsAgreementCommand(String code, boolean agreed) {}

    record SignupAgreements(
            boolean service,
            boolean privacy,
            boolean location,
            boolean age14,
            boolean marketing
    ) {}
}
