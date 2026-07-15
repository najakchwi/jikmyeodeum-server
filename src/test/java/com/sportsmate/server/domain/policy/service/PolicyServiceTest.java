package com.sportsmate.server.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.common.port.out.storage.StoredObject;
import com.sportsmate.server.domain.policy.Terms;
import com.sportsmate.server.domain.policy.TermsAgreement;
import com.sportsmate.server.domain.policy.port.in.PolicyUseCase;
import com.sportsmate.server.domain.policy.port.in.PolicyUseCase.PendingTermReason;
import com.sportsmate.server.domain.policy.port.out.PolicyOutPort;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("PolicyService 단위 테스트")
class PolicyServiceTest {

    private final FakePolicyOutPort policyOutPort = new FakePolicyOutPort();
    private final FakeObjectStorage objectStorage = new FakeObjectStorage();
    private final AuditLogPort auditLogPort = Mockito.mock(AuditLogPort.class);
    private final PolicyService policyService = new PolicyService(policyOutPort, objectStorage, auditLogPort);

    @Test
    @DisplayName("contentKey가 있으면 오브젝트 스토리지에서 약관 본문을 조회하고 캐시한다")
    void getLatestTerm_withContentKey_downloadsAndCachesContent() {
        policyOutPort.terms.add(terms(1L, "service", "2026-06-21", null, "fallback", null));
        objectStorage.objects.put("texts/terms/service/2026-06-21.txt", "downloaded content".getBytes(StandardCharsets.UTF_8));

        var first = policyService.getLatestTerm("service");
        var second = policyService.getLatestTerm("service");

        assertThat(first.content()).isEqualTo("downloaded content");
        assertThat(second.content()).isEqualTo("downloaded content");
        assertThat(objectStorage.downloadCount).isEqualTo(1);
    }

    @Test
    @DisplayName("오브젝트 스토리지 다운로드에 실패하면 inline content로 폴백한다")
    void getLatestTerm_whenDownloadFails_fallsBackToInlineContent() {
        policyOutPort.terms.add(terms(1L, "service", "2026-06-21", null, "fallback content", null));
        objectStorage.failDownload = true;

        var result = policyService.getLatestTerm("service");

        assertThat(result.content()).isEqualTo("fallback content");
    }

    @Test
    @DisplayName("동의 이력이 없거나 다른 버전에 동의한 경우 NEW_VERSION으로 표시한다")
    void getPendingTerms_withoutCurrentAgreement_returnsNewVersionReason() {
        policyOutPort.terms.add(terms(2L, "service", "2026-06-21", null, "service content", null));
        policyOutPort.latestAgreementsByCode.put("service", new TermsAgreement(1L, true, LocalDateTime.now().minusDays(1)));

        var result = policyService.getPendingTerms(10L);

        assertThat(result.terms()).hasSize(1);
        assertThat(result.terms().get(0).reason()).isEqualTo(PendingTermReason.NEW_VERSION);
    }

    @Test
    @DisplayName("같은 버전 동의가 유효기간을 넘긴 경우 EXPIRED로 표시한다")
    void getPendingTerms_withExpiredCurrentAgreement_returnsExpiredReason() {
        policyOutPort.terms.add(terms(1L, "marketing", "2026-06-21", null, "marketing content", 365));
        policyOutPort.latestAgreementsByCode.put("marketing",
                new TermsAgreement(1L, true, LocalDateTime.now().minusDays(366)));

        var result = policyService.getPendingTerms(10L);

        assertThat(result.terms()).hasSize(1);
        assertThat(result.terms().get(0).reason()).isEqualTo(PendingTermReason.EXPIRED);
    }

    @Test
    @DisplayName("같은 버전에 유효하게 동의한 약관은 재동의 목록에서 제외한다")
    void getPendingTerms_withValidCurrentAgreement_excludesTerm() {
        policyOutPort.terms.add(terms(1L, "service", "2026-06-21", null, "service content", null));
        policyOutPort.latestAgreementsByCode.put("service",
                new TermsAgreement(1L, true, LocalDateTime.now().minusDays(1)));

        var result = policyService.getPendingTerms(10L);

        assertThat(result.terms()).isEmpty();
        assertThat(result.hasBlockingRequiredTerms()).isFalse();
    }

    @Test
    @DisplayName("재동의 처리 후 감사 로그에 TERMS_REAGREED를 남긴다")
    void agree_recordsAuditLog() {
        policyOutPort.terms.add(terms(1L, "service", "2026-06-21", null, "service content", null));

        policyService.agree(10L, List.of(new PolicyUseCase.TermsAgreementCommand("service", true)));

        verify(auditLogPort).record(argThat(event ->
                event.category() == AuditCategory.CONSENT
                        && event.action().equals("TERMS_REAGREED")
                        && event.actorId().equals("10")
                        && event.detail().get("codes").equals(List.of("service"))));
    }

    @Test
    @DisplayName("가입 약관 동의 처리 후 감사 로그에 SIGNUP_TERMS_AGREED를 남긴다")
    void recordSignupAgreementsForMember_recordsAuditLog() {
        policyOutPort.terms.add(terms(1L, "service", "2026-06-21", null, "service content", null));
        policyOutPort.terms.add(terms(2L, "privacy", "2026-06-21", null, "privacy content", null));
        policyOutPort.terms.add(terms(3L, "location", "2026-06-21", null, "location content", null));
        policyOutPort.terms.add(terms(4L, "age14", "2026-06-21", null, "age14 content", null));
        policyOutPort.terms.add(terms(5L, "marketing", "2026-06-21", null, "marketing content", null));

        policyService.recordSignupAgreementsForMember(10L,
                new PolicyUseCase.SignupAgreements(true, true, true, true, true));

        verify(auditLogPort).record(argThat(event ->
                event.category() == AuditCategory.CONSENT
                        && event.action().equals("SIGNUP_TERMS_AGREED")
                        && event.actorId().equals("10")
                        && event.detail().get("marketing").equals(true)));
    }

    private Terms terms(Long id, String code, String version, String contentKey, String content, Integer validDays) {
        return new Terms(
                id,
                code,
                version,
                code + " title",
                content,
                contentKey == null ? "texts/terms/" + code + "/" + version + ".txt" : contentKey,
                true,
                validDays,
                LocalDateTime.now().minusDays(1));
    }

    private static class FakePolicyOutPort implements PolicyOutPort {
        private final List<Terms> terms = new ArrayList<>();
        private final Map<String, TermsAgreement> latestAgreementsByCode = new LinkedHashMap<>();

        @Override
        public Optional<Long> findAuthIdByMemberId(Long memberId) {
            return Optional.of(100L + memberId);
        }

        @Override
        public List<Terms> findLatestEffectiveTerms(LocalDateTime now) {
            return terms;
        }

        @Override
        public Optional<Terms> findLatestEffectiveTerm(String code, LocalDateTime now) {
            return terms.stream()
                    .filter(term -> term.code().equals(code))
                    .findFirst();
        }

        @Override
        public Optional<TermsAgreement> findLatestAgreement(Long authId, Long termsId) {
            return latestAgreementsByCode.values().stream()
                    .filter(agreement -> agreement.termsId().equals(termsId))
                    .findFirst();
        }

        @Override
        public Optional<TermsAgreement> findLatestAgreementByCode(Long authId, String code) {
            return Optional.ofNullable(latestAgreementsByCode.get(code));
        }

        @Override
        public void saveAgreement(Long authId, Long termsId, boolean agreed, LocalDateTime agreedAt) {
            latestAgreementsByCode.put(String.valueOf(termsId), new TermsAgreement(termsId, agreed, agreedAt));
        }
    }

    private static class FakeObjectStorage implements ObjectStorage {
        private final Map<String, byte[]> objects = new LinkedHashMap<>();
        private int downloadCount;
        private boolean failDownload;

        @Override
        public StoredObject upload(ObjectUploadCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String objectKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<byte[]> download(String objectKey) {
            downloadCount++;
            if (failDownload) {
                throw new IllegalStateException("download failed");
            }
            return Optional.ofNullable(objects.get(objectKey));
        }

        @Override
        public String getUrl(String objectKey) {
            return "https://example.com/" + objectKey;
        }

        @Override
        public String extractKey(String url) {
            return url;
        }
    }
}
