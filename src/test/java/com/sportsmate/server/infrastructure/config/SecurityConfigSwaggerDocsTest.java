package com.sportsmate.server.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("Swagger 문서 보안 설정 통합 테스트")
class SecurityConfigSwaggerDocsTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @DisplayName("test 프로파일")
    class TestProfile {

        @Autowired MockMvc mockMvc;

        @Test
        @DisplayName("Swagger 관련 경로는 인증 없이 접근할 수 있다")
        void swaggerEndpoints_withoutAuthentication_permitAll() throws Exception {
            mockMvc.perform(get("/api/docs"))
                    .andExpect(status().is3xxRedirection());

            mockMvc.perform(get("/api/swagger-ui/index.html"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isInternalServerError());

            mockMvc.perform(get("/api/api-specs"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("prod")
    @TestPropertySource(properties = {
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.url=jdbc:h2:mem:swagger-docs-prod-test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false",
            "spring.task.scheduling.enabled=false",
            "app.jwt.secret=test-secret-key-for-jwt-signing-must-be-at-least-32-bytes!!",
            "app.kakao.rest-api-key=test-kakao-rest-api-key",
            "app.kakao.app-key=test-kakao-app-key",
            "app.google.client-id=test-google-client-id",
            "app.r2.account-id=test-r2-account-id",
            "app.r2.access-key-id=test-r2-access-key-id",
            "app.r2.secret-access-key=test-r2-secret-access-key",
            "app.r2.bucket=test-r2-bucket",
            "app.r2.public-base-url=https://cdn.example.com",
            "app.swagger.username=docs-user",
            "app.swagger.password=docs-password",
    })
    @DisplayName("prod 프로파일")
    class ProdProfile {

        @Autowired MockMvc mockMvc;

        @Test
        @DisplayName("Swagger 관련 경로는 Basic Auth를 요구한다")
        void swaggerEndpoints_withoutAuthentication_requiresBasicAuth() throws Exception {
            mockMvc.perform(get("/api/docs"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                            .contains("Basic realm=\"Let's Sports Swagger Docs\""));

            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                            .contains("Basic realm=\"Let's Sports Swagger Docs\""));

            mockMvc.perform(get("/api/swagger-ui/index.html"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                            .contains("Basic realm=\"Let's Sports Swagger Docs\""));

            mockMvc.perform(get("/api/api-specs"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                            .contains("Basic realm=\"Let's Sports Swagger Docs\""));
        }

        @Test
        @DisplayName("Swagger 관련 경로는 올바른 Basic Auth 자격증명으로 접근할 수 있다")
        void swaggerEndpoints_withValidBasicAuth_success() throws Exception {
            mockMvc.perform(get("/api/docs").with(httpBasic("docs-user", "docs-password")))
                    .andExpect(status().is3xxRedirection());

            mockMvc.perform(get("/api/swagger-ui/index.html").with(httpBasic("docs-user", "docs-password")))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/swagger-ui/index.html").with(httpBasic("docs-user", "docs-password")))
                    .andExpect(status().isInternalServerError());

            mockMvc.perform(get("/api/api-specs").with(httpBasic("docs-user", "docs-password")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("일반 API 인증 실패는 기존 JSON 에러 포맷을 유지한다")
        void apiEndpoint_withoutAuthentication_usesJsonErrorResponse() throws Exception {
            var response = mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized())
                    .andReturn().getResponse();

            assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
            assertThat(response.getContentType()).contains("application/json");
            assertThat(response.getContentAsString()).contains("\"isSuccess\":false");
        }
    }
}
