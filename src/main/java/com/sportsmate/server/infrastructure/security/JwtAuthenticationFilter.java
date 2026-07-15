package com.sportsmate.server.infrastructure.security;

import com.sportsmate.server.infrastructure.adapter.out.token.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JwtAuthenticationFilter - 요청마다 Bearer 토큰을 검증하고 SecurityContext를 채운다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            jwtProvider.toAuthentication(token).ifPresent(auth -> {
                SecurityContextHolder.getContext().setAuthentication(auth);
                MDC.put("memberId", auth.getName());
            });
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization header에서 Bearer token만 추출한다.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
