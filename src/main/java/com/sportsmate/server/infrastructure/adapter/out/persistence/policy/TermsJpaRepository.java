package com.sportsmate.server.infrastructure.adapter.out.persistence.policy;

import com.sportsmate.server.infrastructure.adapter.out.persistence.member.TermsEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsJpaRepository extends JpaRepository<TermsEntity, Long> {

    Optional<TermsEntity> findFirstByCodeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(
            String code,
            LocalDateTime effectiveAt);

    List<TermsEntity> findAllByEffectiveAtLessThanEqualOrderByCodeAscEffectiveAtDescIdDesc(LocalDateTime effectiveAt);
}
