package com.sportsmate.server.infrastructure.adapter.out.token;

import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.common.port.out.token.TokenIssuer;
import com.sportsmate.server.common.port.out.token.TokenPair;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JwtProvider - JWT 기반 토큰 발급 및 검증을 담당한다.
 */
@Component
public class JwtProvider implements TokenIssuer {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ROLES_CLAIM = "roles";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Override
    public TokenPair issue(String memberId, Role role) {
        String accessToken = buildToken(memberId, List.of(role), ACCESS_TOKEN_TYPE, accessTokenExpiration);
        String refreshToken = buildToken(memberId, List.of(), REFRESH_TOKEN_TYPE, refreshTokenExpiration);
        return new TokenPair(accessToken, refreshToken);
    }

    public String issueServiceToken(String subject, Role role, long expirationSeconds) {
        return buildToken(subject, List.of(role), ACCESS_TOKEN_TYPE, expirationSeconds);
    }

    /**
     * 토큰 서명, 만료, access token 타입을 검증한다.
     */
    public boolean validate(String token) {
        try {
            return ACCESS_TOKEN_TYPE.equals(getClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰 subject에서 멤버 식별자를 추출한다.
     */
    public String extractMemberId(String token) {
        try {
            return getClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 토큰 roles claim에서 멤버 권한 목록을 추출한다.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Object roles = getClaims(token).get(ROLES_CLAIM);
            if (roles instanceof List<?> roleList) {
                return roleList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
            return List.of();
        } catch (JwtException | IllegalArgumentException e) {
            return List.of();
        }
    }

    private String buildToken(String subject, List<Role> roles, String tokenType, long expirationSeconds) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(subject)
                .claim(TOKEN_TYPE_CLAIM, tokenType);

        if (!roles.isEmpty()) {
            builder.claim(ROLES_CLAIM, roles.stream().map(Role::name).toList());
        }

        return builder
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationSeconds * 1000))
                .signWith(key)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
