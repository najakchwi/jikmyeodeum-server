package com.sportsmate.server.infrastructure.adapter.out.token;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtProvider 단위 테스트")
class JwtProviderTest {

    private static final String SECRET = "test-jwt-secret-key-please-change-0123456789";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3600L, 2592000L);

    @Test
    @DisplayName("issue로 발급한 access token은 validate를 통과한다")
    void issue_accessToken_isValid() {
        var tokenPair = jwtProvider.issue("member-1", Role.USER);

        assertThat(jwtProvider.validate(tokenPair.accessToken())).isTrue();
    }

    @Test
    @DisplayName("issue로 발급한 refresh token은 validate를 통과하지 못한다")
    void issue_refreshToken_isNotValidAsAccessToken() {
        var tokenPair = jwtProvider.issue("member-1", Role.USER);

        assertThat(jwtProvider.validate(tokenPair.refreshToken())).isFalse();
    }

    @Test
    @DisplayName("access token에서 memberId를 추출할 수 있다")
    void extractMemberId_fromAccessToken_returnsMemberId() {
        var tokenPair = jwtProvider.issue("member-1", Role.USER);

        assertThat(jwtProvider.extractMemberId(tokenPair.accessToken())).isEqualTo("member-1");
    }

    @Test
    @DisplayName("access token에서 roles를 추출할 수 있다")
    void extractRoles_fromAccessToken_returnsRoles() {
        var tokenPair = jwtProvider.issue("member-1", Role.ADMIN);

        assertThat(jwtProvider.extractRoles(tokenPair.accessToken())).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("refresh token에는 roles claim이 비어 있다")
    void extractRoles_fromRefreshToken_returnsEmpty() {
        var tokenPair = jwtProvider.issue("member-1", Role.USER);

        assertThat(jwtProvider.extractRoles(tokenPair.refreshToken())).isEmpty();
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 validate에서 false를 반환한다")
    void validate_withMalformedToken_returnsFalse() {
        assertThat(jwtProvider.validate("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 validate에서 false를 반환한다")
    void validate_withTokenSignedByDifferentKey_returnsFalse() {
        SecretKey otherKey = Keys.hmacShaKeyFor("other-jwt-secret-key-please-change-0123".getBytes());
        String token = Jwts.builder()
                .subject("member-1")
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(otherKey)
                .compact();

        assertThat(jwtProvider.validate(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 validate에서 false를 반환한다")
    void validate_withExpiredToken_returnsFalse() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String expiredToken = Jwts.builder()
                .subject("member-1")
                .claim("type", "access")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(key)
                .compact();

        assertThat(jwtProvider.validate(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("서비스 access token은 validate를 통과한다")
    void issueServiceToken_validAccessToken_isValid() {
        String token = jwtProvider.issueServiceToken("admin-service", Role.ADMIN, 3600L);

        assertThat(jwtProvider.validate(token)).isTrue();
    }

    @Test
    @DisplayName("서비스 access token에서 지정한 role을 추출할 수 있다")
    void extractRoles_fromServiceToken_returnsRole() {
        String token = jwtProvider.issueServiceToken("admin-service", Role.ADMIN, 3600L);

        assertThat(jwtProvider.extractRoles(token)).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("서비스 access token에서 subject 문자열을 그대로 추출할 수 있다")
    void extractMemberId_fromServiceToken_returnsSubject() {
        String token = jwtProvider.issueServiceToken("admin-service", Role.ADMIN, 3600L);

        assertThat(jwtProvider.extractMemberId(token)).isEqualTo("admin-service");
    }

    @Test
    @DisplayName("이미 만료된 서비스 access token은 validate에서 false를 반환한다")
    void validate_withExpiredServiceToken_returnsFalse() {
        String token = jwtProvider.issueServiceToken("admin-service", Role.ADMIN, -1L);

        assertThat(jwtProvider.validate(token)).isFalse();
    }
}
