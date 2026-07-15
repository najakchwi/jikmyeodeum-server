package com.sportsmate.server.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import com.sportsmate.server.infrastructure.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

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
        "/ws",
        "/ws/**",
        "/actuator/health",
        "/actuator/health/**",
        "/error",
    };

    private static final String[] SWAGGER_ENDPOINTS = {
        "/api/docs",
        "/api/swagger-ui/**",
        "/swagger-ui/**",
        "/api/api-specs/**",
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final boolean prodProfile;
    private final String swaggerUsername;
    private final String swaggerPassword;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${app.swagger.username:}") String swaggerUsername,
            @Value("${app.swagger.password:}") String swaggerPassword) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.prodProfile = environment.matchesProfiles("prod");
        this.swaggerUsername = swaggerUsername;
        this.swaggerPassword = swaggerPassword;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, CommonErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(auth -> {
                    if (prodProfile) {
                        auth.requestMatchers(SWAGGER_ENDPOINTS).authenticated();
                    } else {
                        auth.requestMatchers(SWAGGER_ENDPOINTS).permitAll();
                    }
                    auth.requestMatchers(PERMIT_ALL_ENDPOINTS).permitAll()
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (prodProfile) {
            http
                    .userDetailsService(userDetailsService())
                    .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(authenticationEntryPoint()));
        } else {
            http.httpBasic(AbstractHttpConfigurer::disable);
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        if (!prodProfile) {
            return new InMemoryUserDetailsManager();
        }

        return new InMemoryUserDetailsManager(User.withUsername(swaggerUsername)
                .password(passwordEncoder().encode(swaggerPassword))
                .roles("SWAGGER_DOCS")
                .build());
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

    private AuthenticationEntryPoint authenticationEntryPoint() {
        if (!prodProfile) {
            return (request, response, authException) ->
                    writeErrorResponse(response, CommonErrorCode.UNAUTHORIZED);
        }

        BasicAuthenticationEntryPoint basicEntryPoint = new BasicAuthenticationEntryPoint();
        basicEntryPoint.setRealmName("Let's Sports Swagger Docs");

        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(swaggerRequestMatcher(), basicEntryPoint);

        DelegatingAuthenticationEntryPoint delegatingEntryPoint = new DelegatingAuthenticationEntryPoint(entryPoints);
        delegatingEntryPoint.setDefaultEntryPoint((request, response, authException) ->
                writeErrorResponse(response, CommonErrorCode.UNAUTHORIZED));
        return delegatingEntryPoint;
    }

    private RequestMatcher swaggerRequestMatcher() {
        return new OrRequestMatcher(java.util.Arrays.stream(SWAGGER_ENDPOINTS)
                .map(PathPatternRequestMatcher::pathPattern)
                .toList());
    }
}
