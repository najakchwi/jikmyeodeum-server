package com.sportsmate.server.domain.policy.port.out;

import com.sportsmate.server.domain.policy.Terms;
import com.sportsmate.server.domain.policy.TermsAgreement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PolicyOutPort {

    Optional<Long> findAuthIdByMemberId(Long memberId);

    List<Terms> findLatestEffectiveTerms(LocalDateTime now);

    Optional<Terms> findLatestEffectiveTerm(String code, LocalDateTime now);

    Optional<TermsAgreement> findLatestAgreement(Long authId, Long termsId);

    Optional<TermsAgreement> findLatestAgreementByCode(Long authId, String code);

    void saveAgreement(Long authId, Long termsId, boolean agreed, LocalDateTime agreedAt);
}
