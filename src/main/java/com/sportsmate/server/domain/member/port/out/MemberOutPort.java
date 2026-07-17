package com.sportsmate.server.domain.member.port.out;

import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.MemberLeagueProfile;
import com.sportsmate.server.domain.member.enums.LoginType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberOutPort {
    Member save(Member member);
    Optional<Member> findById(Long id);
    Optional<Member> findByPhone(String phone);
    Optional<Member> findByProvider(LoginType loginType, String providerId);
    Optional<String> findExpoPushTokenById(Long id);
    boolean isWelcomeNotified(Long id);
    boolean existsByPhone(String phone);
    boolean existsByNickname(String nickname);
    void updateExpoPushToken(Long id, String expoPushToken);
    boolean markWelcomeNotified(Long id);
    void withdraw(Long id);
    default boolean existsLeagueProfile(Long memberId, Long leagueId) {
        return false;
    }

    default MemberLeagueProfile upsertLeagueProfile(Long memberId, MemberLeagueProfile leagueProfile) {
        throw new UnsupportedOperationException("upsertLeagueProfile is not implemented");
    }
    default List<LinkedAccount> findLinkedAccounts(Long memberId) {
        throw new UnsupportedOperationException("findLinkedAccounts is not implemented");
    }

    default Optional<Long> findLinkedMemberId(LoginType loginType, String providerId) {
        throw new UnsupportedOperationException("findLinkedMemberId is not implemented");
    }

    default boolean hasLoginMethod(Long memberId, LoginType loginType) {
        throw new UnsupportedOperationException("hasLoginMethod is not implemented");
    }

    default int countLoginMethods(Long memberId) {
        throw new UnsupportedOperationException("countLoginMethods is not implemented");
    }

    default void linkSocialAccount(Long memberId, LoginType loginType, String providerId) {
        throw new UnsupportedOperationException("linkSocialAccount is not implemented");
    }

    default void linkPhoneAccount(Long memberId, String encodedPassword) {
        throw new UnsupportedOperationException("linkPhoneAccount is not implemented");
    }

    default void unlinkLoginMethod(Long memberId, LoginType loginType) {
        throw new UnsupportedOperationException("unlinkLoginMethod is not implemented");
    }

    default void changePhone(Long memberId, String phone) {
        throw new UnsupportedOperationException("changePhone is not implemented");
    }

    record LinkedAccount(LoginType loginType, boolean linked, LocalDateTime linkedAt) {}
}
