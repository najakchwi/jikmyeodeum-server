package com.sportsmate.server.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import com.sportsmate.server.infrastructure.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SecurityConfig - Stateless JWT 기반 보안 설정을 구성한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PERMIT_ALL_ENDPOINTS = {
        "/api/v1/auth/**",
        "/api/v1/banners",
        "/api/v1/faqs",
        "/api/v1/terms/service",
        "/api/v1/terms/privacy",
        "/api/v1/terms/location",
        "/api/v1/terms/age14",
        "/api/v1/terms/marketing",
        "/api/v1/content/bootstrap",
        "/api/v1/files/**",
        "/api/v1/games/**",
        "/api/v1/teams",
        "/api/swagger-ui/**",
        "/swagger-ui/**",
        "/api/swagger-ui.html",
        "/api/api-specs/**",
        "/actuator/health",
        "/actuator/health/**",
        "/error",
    };


    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(response, CommonErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, CommonErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 예외를 공통 ApiResponse 포맷으로 직렬화한다.
     */
    private void writeErrorResponse(HttpServletResponse response, CommonErrorCode errorCode) throws java.io.IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode).getBody());
    }
}
