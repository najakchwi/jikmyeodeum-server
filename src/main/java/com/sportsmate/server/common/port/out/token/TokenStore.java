package com.sportsmate.server.common.port.out.token;

import java.util.Optional;

public interface TokenStore {

    void save(String memberId, String refreshToken, long expiresInSeconds);

    Optional<String> findMemberIdByRefreshToken(String refreshToken);

    void deleteByMemberId(String memberId);
}
