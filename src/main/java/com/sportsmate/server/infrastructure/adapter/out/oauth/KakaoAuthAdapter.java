package com.sportsmate.server.infrastructure.adapter.out.oauth;

import com.sportsmate.server.common.port.out.oauth.InvalidSocialTokenException;
import com.sportsmate.server.common.port.out.oauth.KakaoAuthPort;
import com.sportsmate.server.common.port.out.oauth.SocialUserInfo;
import com.sportsmate.server.infrastructure.monitoring.ExternalDependencyMonitor;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * KakaoAuthAdapter - 카카오 JWKS를 이용해 ID Token 서명, 만료, iss, aud를 검증한다.
 */
@Component
public class KakaoAuthAdapter implements KakaoAuthPort {

    private final NimbusJwtDecoder jwtDecoder;
    private final ExternalDependencyMonitor externalDependencyMonitor;

    public KakaoAuthAdapter(
            @Value("${app.kakao.app-key}") String appKey,
            @Value("${app.kakao.jwks-uri}") String jwksUri,
            @Value("${app.kakao.issuer}") String issuer,
            ExternalDependencyMonitor externalDependencyMonitor) {
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        this.jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(appKey))));
        this.externalDependencyMonitor = externalDependencyMonitor;
    }

    @Override
    public SocialUserInfo verify(String idToken) {
        Jwt jwt;
        try {
            jwt = externalDependencyMonitor.observe("kakao-oauth", () -> jwtDecoder.decode(idToken));
        } catch (JwtException e) {
            throw new InvalidSocialTokenException(e.getMessage());
        }

        String providerId = jwt.getSubject();
        if (providerId == null) {
            throw new InvalidSocialTokenException("Missing sub claim");
        }

        // email·nickname 은 카카오 개발자 콘솔에서 해당 스코프를 활성화하고
        // 사용자가 동의한 경우에만 ID Token에 포함되므로 선택적으로 처리한다.
        String email = jwt.getClaimAsString("email");
        String name  = jwt.getClaimAsString("nickname");

        return new SocialUserInfo(providerId, email, name);
    }
}
