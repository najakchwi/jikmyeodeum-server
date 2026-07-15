package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.domain.member.enums.LoginType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {
    @Query("""
            select m
            from MemberEntity m
            where m.phone = :phone
              and m.deletedAt is null
              and exists (
                  select 1
                  from AuthEntity a
                  where (a.memberId = m.id or a.id = m.authId)
                    and a.loginType = 'PHONE'
                    and a.status = 'ACTIVE'
              )
            """)
    Optional<MemberEntity> findByPhone(@Param("phone") String phone);

    @Query("""
            select m
            from MemberEntity m
            where m.deletedAt is null
              and (
                  m.id in (
                      select a.memberId
                      from AuthEntity a
                      where a.loginType = :loginType
                        and a.providerId = :providerId
                        and a.status = 'ACTIVE'
                        and a.memberId is not null
                  )
                  or m.authId in (
                      select a.id
                      from AuthEntity a
                      where a.loginType = :loginType
                        and a.providerId = :providerId
                        and a.status = 'ACTIVE'
                  )
              )
            """)
    Optional<MemberEntity> findByLoginTypeAndProviderId(
            @Param("loginType") String loginType,
            @Param("providerId") String providerId);

    @Query("""
            select count(m) > 0
            from MemberEntity m
            where m.phone = :phone
              and m.deletedAt is null
            """)
    boolean existsByPhone(@Param("phone") String phone);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MemberEntity m
            set m.phone = :phone,
                m.phoneVerifiedAt = CURRENT_TIMESTAMP,
                m.updatedAt = CURRENT_TIMESTAMP
            where m.id = :id
            """)
    int updatePhone(@Param("id") Long id, @Param("phone") String phone);

    @Query("""
            select count(m) > 0
            from MemberEntity m
            where m.nickname = :nickname
              and m.deletedAt is null
            """)
    boolean existsByNickname(@Param("nickname") String nickname);

    @Query("""
            select m.expoPushToken
            from MemberEntity m
            where m.id = :id
            """)
    Optional<String> findExpoPushTokenById(@Param("id") Long id);

    @Query("""
            select m.welcomeNotified
            from MemberEntity m
            where m.id = :id
            """)
    boolean isWelcomeNotified(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MemberEntity m
            set m.expoPushToken = :expoPushToken,
                m.updatedAt = CURRENT_TIMESTAMP
            where m.id = :id
            """)
    int updateExpoPushToken(
            @Param("id") Long id,
            @Param("expoPushToken") String expoPushToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MemberEntity m
            set m.welcomeNotified = true,
                m.updatedAt = CURRENT_TIMESTAMP
            where m.id = :id
              and m.welcomeNotified = false
            """)
    int markWelcomeNotified(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MemberEntity m
            set m.nickname = :nickname,
                m.phone = 'DELETED',
                m.phoneVerifiedAt = null,
                m.birthYear = null,
                m.gender = null,
                m.profileImageKey = null,
                m.bio = null,
                m.expoPushToken = null,
                m.deletedAt = CURRENT_TIMESTAMP,
                m.updatedAt = CURRENT_TIMESTAMP
            where m.id = :id
            """)
    int anonymize(@Param("id") Long id, @Param("nickname") String nickname);

    default Optional<MemberEntity> findByLoginTypeAndProviderId(
            LoginType loginType,
            String providerId
    ) {
        return findByLoginTypeAndProviderId(loginType.name(), providerId);
    }
}
