package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthJpaRepository extends JpaRepository<AuthEntity, Long> {

    Optional<AuthEntity> findByPhone(String phone);

    boolean existsByPhone(String phone);

    Optional<AuthEntity> findByMemberIdAndLoginType(Long memberId, String loginType);

    Optional<AuthEntity> findByLoginTypeAndProviderId(String loginType, String providerId);

    List<AuthEntity> findAllByMemberIdAndStatus(Long memberId, String status);

    boolean existsByMemberIdAndLoginTypeAndStatus(Long memberId, String loginType, String status);

    int countByMemberIdAndStatus(Long memberId, String status);

    void deleteByMemberIdAndLoginType(Long memberId, String loginType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthEntity a
            set a.memberId = :memberId,
                a.updatedAt = CURRENT_TIMESTAMP
            where a.id = :authId
            """)
    int attachMember(@Param("authId") Long authId, @Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthEntity a
            set a.phone = null,
                a.password = null,
                a.providerId = null,
                a.memberId = null,
                a.status = 'WITHDRAWN',
                a.updatedAt = CURRENT_TIMESTAMP
            where a.id = :id
            """)
    int withdraw(@Param("id") Long id);
}
