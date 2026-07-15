package com.sportsmate.server.infrastructure.config;

import com.sportsmate.server.infrastructure.adapter.in.web.common.swagger.customizer.ExampleResponseCustomizer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH_SECURITY_SCHEME = "access-token";

    private final String serverUrl;
    private final Resource swaggerDescription;

    public SwaggerConfig(
            @Value("${app.server.url}") String serverUrl,
            @Value("classpath:/swagger/description.md") Resource swaggerDescription) {
        this.serverUrl = serverUrl;
        this.swaggerDescription = swaggerDescription;
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url(serverUrl))
                .components(securityComponents())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SECURITY_SCHEME))
                .addTagsItem(new Tag().name("Auth").description("회원가입, 로그인, 토큰 재발급, 비밀번호 변경 API"))
                .addTagsItem(new Tag().name("Member").description("내 프로필, 응원 성향, 동행 선호, 위치 인증, 아바타 API"))
                .addTagsItem(new Tag().name("Game").description("KBO 경기 일정, 경기 상세, 구단 목록 API"))
                .addTagsItem(new Tag().name("Application").description("직관 동행 신청, 신청 현황, 매칭 수락/거절 API"))
                .addTagsItem(new Tag().name("Chat").description("매칭 채팅방과 메시지 API"))
                .addTagsItem(new Tag().name("Notification").description("알림 목록, 읽음 처리, 알림 설정, 푸시 토큰 API"))
                .addTagsItem(new Tag().name("Review").description("동행 평가 API"))
                .addTagsItem(new Tag().name("Report").description("사용자 신고 API"))
                .addTagsItem(new Tag().name("Static Content").description("배너, FAQ, 약관, 이미지 파일 API"))
                .addTagsItem(new Tag().name("Admin").description("운영자 경기 데이터 동기화 API"))
                .info(apiInfo());
    }

    @Bean
    public GroupedOpenApi generalApi(ExampleResponseCustomizer exampleResponseCustomizer) {
        return GroupedOpenApi.builder()
                .group("general")
                .pathsToMatch("/api/**")
                .addOperationCustomizer(exampleResponseCustomizer)
                .build();
    }

    private Info apiInfo() {
        return new Info()
                .title("직며듦 Project API")
                .description(loadDescription())
                .version("0.0.1");
    }

    private String loadDescription() {
        try {
            return StreamUtils.copyToString(swaggerDescription.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes(
                        BEARER_AUTH_SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
    }
}
