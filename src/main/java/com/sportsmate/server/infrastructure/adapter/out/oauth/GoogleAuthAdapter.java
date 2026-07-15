package com.sportsmate.server.infrastructure.adapter.out.oauth;

import com.sportsmate.server.common.port.out.oauth.GoogleAuthPort;
import com.sportsmate.server.common.port.out.oauth.InvalidSocialTokenException;
import com.sportsmate.server.common.port.out.oauth.SocialUserInfo;
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
 * GoogleAuthAdapter - 구글 JWKS를 이용해 ID Token 서명, 만료, iss, aud를 검증한다.
 */
@Component
public class GoogleAuthAdapter implements GoogleAuthPort {

    private final NimbusJwtDecoder jwtDecoder;

    public GoogleAuthAdapter(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.jwks-uri}") String jwksUri,
            @Value("${app.google.issuer}") String issuer) {
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        this.jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(clientId))));
    }

    @Override
    public SocialUserInfo verify(String idToken) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException e) {
            throw new InvalidSocialTokenException(e.getMessage());
        }

        String providerId = jwt.getSubject();
        if (providerId == null) {
            throw new InvalidSocialTokenException("Missing sub claim");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) {
            throw new InvalidSocialTokenException("Missing email claim");
        }

        String name = jwt.getClaimAsString("name");
        if (name == null) {
            throw new InvalidSocialTokenException("Missing name claim");
        }

        return new SocialUserInfo(providerId, email, name);
    }
}
