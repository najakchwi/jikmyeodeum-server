package com.sportsmate.server.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("핵심 API 흐름 통합 테스트")
class ApiFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("휴대폰 인증부터 회원가입과 내 정보 조회까지 성공한다")
    void signupAndGetMe_success() throws Exception {
        String phone = "01098765432";
        mockMvc.perform(post("/api/v1/auth/signup/phone/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", phone))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresIn").value(180));

        String verifyBody = mockMvc.perform(post("/api/v1/auth/signup/phone/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("phone", phone, "code", "000000"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String signupToken = objectMapper.readTree(verifyBody).path("data").path("signupToken").asText();

        Map<String, Object> signup = Map.ofEntries(
                Map.entry("signupToken", signupToken),
                Map.entry("password", "password123"),
                Map.entry("profile", Map.of(
                        "nickname", "통합테스트",
                        "birthdate", "1997-03-15",
                        "gender", "female")),
                Map.entry("agreements", Map.of(
                        "service", true,
                        "privacy", true,
                        "location", true,
                        "age14", true,
                        "marketing", false)),
                Map.entry("team", "LG"),
                Map.entry("watchStyles", new String[]{"cheer"}),
                Map.entry("personality", "tension"),
                Map.entry("talkStyle", "talkative"),
                Map.entry("smokingStatus", "non-smoker"),
                Map.entry("genderPref", "any"),
                Map.entry("agePref", "any"),
                Map.entry("smokingPref", "non-smoker"),
                Map.entry("distanceKm", 20),
                Map.entry("location", Map.of("verified", false)));
        String signupBody = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.nickname").value("통합테스트"))
                .andExpect(jsonPath("$.data.user.gender").value("female"))
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(signupBody).path("data");
        String accessToken = data.path("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginType").value("phone"))
                .andExpect(jsonPath("$.data.team").value("LG"));
    }

    @Test
    @DisplayName("경기 캘린더는 인증 없이 조회할 수 있다")
    void gameCalendar_withoutAuthentication_success() throws Exception {
        LocalDate start = LocalDate.now();
        mockMvc.perform(get("/api/v1/games/calendar")
                        .queryParam("startDate", start.toString())
                        .queryParam("endDate", start.plusDays(14).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dates").isArray())
                .andExpect(jsonPath("$.data.seasonStartDate").isNotEmpty())
                .andExpect(jsonPath("$.data.seasonEndDate").isNotEmpty());
    }

    @Test
    @DisplayName("경기 목록과 상세는 인증 없이 조회할 수 있다")
    void games_withoutAuthentication_success() throws Exception {
        LocalDate gameDate = LocalDate.now().plusDays(3);

        mockMvc.perform(get("/api/v1/games")
                        .queryParam("date", gameDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].homeTeamEmblemUrl").isNotEmpty())
                .andExpect(jsonPath("$.data[0].awayTeamEmblemUrl").isNotEmpty());

        mockMvc.perform(get("/api/v1/games/g1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("g1"))
                .andExpect(jsonPath("$.data.homeTeamEmblemUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.awayTeamEmblemUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.applicationId").doesNotExist());
    }

    @Test
    @DisplayName("팀 목록은 인증 없이 조회할 수 있고 엠블럼 URL을 포함한다")
    void teams_withoutAuthentication_success() throws Exception {
        mockMvc.perform(get("/api/v1/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].name").isNotEmpty())
                .andExpect(jsonPath("$.data[0].shortName").isNotEmpty())
                .andExpect(jsonPath("$.data[0].emblemUrl").isNotEmpty());
    }

    @Test
    @DisplayName("헬스 체크와 liveness/readiness probe는 인증 없이 조회할 수 있다")
    void actuatorHealth_withoutAuthentication_success() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
