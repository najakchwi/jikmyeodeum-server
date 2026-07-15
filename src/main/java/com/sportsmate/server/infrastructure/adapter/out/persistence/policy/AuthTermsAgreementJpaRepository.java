package com.sportsmate.server.infrastructure.adapter.out.persistence.policy;

import com.sportsmate.server.infrastructure.adapter.out.persistence.member.AuthTermsAgreementEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthTermsAgreementJpaRepository extends JpaRepository<AuthTermsAgreementEntity, Long> {

    Optional<AuthTermsAgreementEntity> findFirstByAuthIdAndTermsIdOrderByAgreedAtDescIdDesc(Long authId, Long termsId);

    @Query("""
            SELECT agreement
            FROM AuthTermsAgreementEntity agreement
            JOIN TermsEntity terms ON terms.id = agreement.termsId
            WHERE agreement.authId = :authId
              AND terms.code = :code
            ORDER BY agreement.agreedAt DESC, agreement.id DESC
            """)
    List<AuthTermsAgreementEntity> findLatestByAuthIdAndTermsCode(
            @Param("authId") Long authId,
            @Param("code") String code,
            Pageable pageable);
}
