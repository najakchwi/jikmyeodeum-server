package com.sportsmate.server.infrastructure.adapter.out.token;

import com.sportsmate.server.common.port.out.token.TokenStore;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * InMemoryTokenStoreAdapter - refresh token을 메모리에 저장한다.
 * 추후 RedisTokenStoreAdapter로 교체 가능하다.
 */
@Component
@ConditionalOnProperty(name = "app.store.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenStoreAdapter implements TokenStore {

    // memberId -> refreshToken
    private final ConcurrentHashMap<String, String> memberToToken = new ConcurrentHashMap<>();

    // refreshToken -> entry
    private final ConcurrentHashMap<String, TokenEntry> tokenToMember = new ConcurrentHashMap<>();

    @Override
    public synchronized void save(String memberId, String refreshToken, long expiresInSeconds) {
        deleteByMemberId(memberId);
        memberToToken.put(memberId, refreshToken);
        tokenToMember.put(refreshToken, new TokenEntry(memberId, Instant.now().plusSeconds(expiresInSeconds)));
    }

    @Override
    public Optional<String> findMemberIdByRefreshToken(String refreshToken) {
        TokenEntry entry = tokenToMember.get(refreshToken);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(Instant.now())) {
            tokenToMember.remove(refreshToken);
            memberToToken.remove(entry.memberId(), refreshToken);
            return Optional.empty();
        }
        return Optional.of(entry.memberId());
    }

    @Override
    public synchronized void deleteByMemberId(String memberId) {
        String refreshToken = memberToToken.remove(memberId);
        if (refreshToken != null) {
            tokenToMember.remove(refreshToken);
        }
    }

    private record TokenEntry(String memberId, Instant expiresAt) {}
}
