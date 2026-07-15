package com.sportsmate.server.infrastructure.adapter.out.persistence.policy;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.policy.Terms;
import com.sportsmate.server.domain.policy.TermsAgreement;
import com.sportsmate.server.domain.policy.port.out.PolicyOutPort;
import com.sportsmate.server.infrastructure.adapter.out.persistence.member.AuthTermsAgreementEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.member.MemberJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.member.TermsEntity;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

@PersistenceAdapter
public class PolicyPersistenceAdapter implements PolicyOutPort {

    private final MemberJpaRepository memberRepository;
    private final TermsJpaRepository termsRepository;
    private final AuthTermsAgreementJpaRepository agreementRepository;

    public PolicyPersistenceAdapter(
            MemberJpaRepository memberRepository,
            TermsJpaRepository termsRepository,
            AuthTermsAgreementJpaRepository agreementRepository
    ) {
        this.memberRepository = memberRepository;
        this.termsRepository = termsRepository;
        this.agreementRepository = agreementRepository;
    }

    @Override
    public Optional<Long> findAuthIdByMemberId(Long memberId) {
        return memberRepository.findById(memberId).map(member -> member.getAuthId());
    }

    @Override
    public List<Terms> findLatestEffectiveTerms(LocalDateTime now) {
        LinkedHashMap<String, Terms> latestByCode = new LinkedHashMap<>();
        for (TermsEntity entity : termsRepository.findAllByEffectiveAtLessThanEqualOrderByCodeAscEffectiveAtDescIdDesc(now)) {
            latestByCode.putIfAbsent(entity.getCode(), toDomain(entity));
        }
        return latestByCode.values().stream().toList();
    }

    @Override
    public Optional<Terms> findLatestEffectiveTerm(String code, LocalDateTime now) {
        return termsRepository.findFirstByCodeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(code, now)
                .map(this::toDomain);
    }

    @Override
    public Optional<TermsAgreement> findLatestAgreement(Long authId, Long termsId) {
        return agreementRepository.findFirstByAuthIdAndTermsIdOrderByAgreedAtDescIdDesc(authId, termsId)
                .map(this::toDomain);
    }

    @Override
    public Optional<TermsAgreement> findLatestAgreementByCode(Long authId, String code) {
        return agreementRepository.findLatestByAuthIdAndTermsCode(authId, code, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public void saveAgreement(Long authId, Long termsId, boolean agreed, LocalDateTime agreedAt) {
        agreementRepository.save(AuthTermsAgreementEntity.builder()
                .authId(authId)
                .termsId(termsId)
                .agreed(agreed)
                .agreedAt(agreedAt)
                .build());
    }

    private Terms toDomain(TermsEntity entity) {
        return new Terms(
                entity.getId(),
                entity.getCode(),
                entity.getVersion(),
                entity.getTitle(),
                entity.getContent(),
                entity.getContentKey(),
                Boolean.TRUE.equals(entity.getRequired()),
                entity.getValidDays(),
                entity.getEffectiveAt());
    }

    private TermsAgreement toDomain(AuthTermsAgreementEntity entity) {
        return new TermsAgreement(entity.getTermsId(), entity.getAgreed(), entity.getAgreedAt());
    }
}
