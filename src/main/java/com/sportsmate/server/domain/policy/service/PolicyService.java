package com.sportsmate.server.domain.policy.service;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditEvent;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.common.port.out.audit.AuditResult;
import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.domain.member.exception.MemberErrorCode;
import com.sportsmate.server.domain.policy.Terms;
import com.sportsmate.server.domain.policy.TermsAgreement;
import com.sportsmate.server.domain.policy.port.in.PolicyUseCase;
import com.sportsmate.server.domain.policy.port.out.PolicyOutPort;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PolicyService implements PolicyUseCase {

    private final PolicyOutPort policyOutPort;
    private final ObjectStorage objectStorage;
    private final AuditLogPort auditLogPort;
    private final ConcurrentMap<String, String> termsContentCache = new ConcurrentHashMap<>();

    public PolicyService(PolicyOutPort policyOutPort, ObjectStorage objectStorage, AuditLogPort auditLogPort) {
        this.policyOutPort = policyOutPort;
        this.objectStorage = objectStorage;
        this.auditLogPort = auditLogPort;
    }

    @Override
    public TermsContent getLatestTerm(String code) {
        Terms terms = latestTerms(code, LocalDateTime.now());
        return toContent(terms);
    }

    @Override
    public PendingTermsResult getPendingTerms(Long memberId) {
        Long authId = authId(memberId);
        LocalDateTime now = LocalDateTime.now();
        List<PendingTerm> pending = policyOutPort.findLatestEffectiveTerms(now).stream()
                .map(terms -> new PendingTermWithReason(terms, pendingReason(authId, terms, now)))
                .filter(pendingTerm -> pendingTerm.reason().isPresent())
                .map(terms -> new PendingTerm(
                        terms.terms().id(),
                        terms.terms().code(),
                        terms.terms().version(),
                        terms.terms().title(),
                        resolveContent(terms.terms()),
                        terms.terms().contentKey(),
                        terms.reason().orElseThrow(),
                        terms.terms().required(),
                        terms.terms().required(),
                        terms.terms().validDays(),
                        terms.terms().effectiveAt()))
                .toList();
        return new PendingTermsResult(pending.stream().anyMatch(PendingTerm::blocking), pending);
    }

    @Override
    @Transactional
    public void agree(Long memberId, List<TermsAgreementCommand> agreements) {
        Long authId = authId(memberId);
        recordAgreements(authId, agreements);
        auditLogPort.record(AuditEvent.of(
                AuditCategory.CONSENT, "TERMS_REAGREED", "MEMBER", memberId.toString(),
                "MEMBER", memberId.toString(), AuditResult.SUCCESS,
                Map.of("codes", agreements.stream().map(TermsAgreementCommand::code).toList())));
    }

    @Override
    @Transactional
    public void recordSignupAgreementsForMember(Long memberId, SignupAgreements agreements) {
        recordSignupAgreements(authId(memberId), agreements);
        auditLogPort.record(AuditEvent.of(
                AuditCategory.CONSENT, "SIGNUP_TERMS_AGREED", "MEMBER", memberId.toString(),
                "MEMBER", memberId.toString(), AuditResult.SUCCESS,
                Map.of("marketing", agreements.marketing())));
    }

    @Override
    @Transactional
    public void recordSignupAgreements(Long authId, SignupAgreements agreements) {
        if (!agreements.service() || !agreements.privacy() || !agreements.location() || !agreements.age14()) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        recordAgreements(authId, List.of(
                new TermsAgreementCommand("service", agreements.service()),
                new TermsAgreementCommand("privacy", agreements.privacy()),
                new TermsAgreementCommand("location", agreements.location()),
                new TermsAgreementCommand("age14", agreements.age14()),
                new TermsAgreementCommand("marketing", agreements.marketing())));
    }

    private void recordAgreements(Long authId, List<TermsAgreementCommand> agreements) {
        LocalDateTime now = LocalDateTime.now();
        for (TermsAgreementCommand agreement : agreements) {
            Terms terms = latestTerms(agreement.code(), now);
            if (terms.required() && !agreement.agreed()) {
                throw new BusinessException(CommonErrorCode.INVALID_INPUT);
            }
            policyOutPort.saveAgreement(authId, terms.id(), agreement.agreed(), now);
        }
    }

    private Optional<PendingTermReason> pendingReason(Long authId, Terms terms, LocalDateTime now) {
        Optional<TermsAgreement> latestAgreement = policyOutPort.findLatestAgreementByCode(authId, terms.code());
        if (latestAgreement.isEmpty()) {
            return Optional.of(PendingTermReason.NEW_VERSION);
        }

        TermsAgreement agreement = latestAgreement.get();
        if (!agreement.agreed() || !terms.id().equals(agreement.termsId())) {
            return Optional.of(PendingTermReason.NEW_VERSION);
        }
        if (!terms.isAgreementValid(agreement.agreedAt(), now)) {
            return Optional.of(PendingTermReason.EXPIRED);
        }
        return Optional.empty();
    }

    private Terms latestTerms(String code, LocalDateTime now) {
        return policyOutPort.findLatestEffectiveTerm(code, now)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_INPUT));
    }

    private Long authId(Long memberId) {
        return policyOutPort.findAuthIdByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private TermsContent toContent(Terms terms) {
        return new TermsContent(
                terms.code(),
                terms.version(),
                terms.title(),
                resolveContent(terms),
                terms.contentKey(),
                terms.required(),
                terms.validDays(),
                terms.effectiveAt());
    }

    private String resolveContent(Terms terms) {
        if (hasText(terms.contentKey())) {
            String cachedContent = termsContentCache.get(terms.contentKey());
            if (cachedContent != null) {
                return cachedContent;
            }

            try {
                Optional<byte[]> downloaded = objectStorage.download(terms.contentKey());
                if (downloaded.isPresent()) {
                    String content = new String(downloaded.get(), StandardCharsets.UTF_8);
                    termsContentCache.putIfAbsent(terms.contentKey(), content);
                    return content;
                }
            } catch (RuntimeException ignored) {
                // Inline content is the final fallback for unavailable object storage.
            }
        }

        if (hasText(terms.content())) {
            return terms.content();
        }
        throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Terms content is missing: " + terms.code());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record PendingTermWithReason(Terms terms, Optional<PendingTermReason> reason) {
    }
}
