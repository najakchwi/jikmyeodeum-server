package com.sportsmate.server.infrastructure.adapter.in.web.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("아바타 업로드 API 통합 테스트")
class MemberAvatarUploadIntegrationTest {

    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger();

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("이미지를 업로드하면 아바타 URL이 갱신되고 파일을 다시 내려받을 수 있다")
    void uploadAvatar_validImage_updatesProfileAndServesFile() throws Exception {
        String accessToken = signUp();
        byte[] imageBytes = "fake-png-bytes".getBytes();
        MockMultipartFile image = new MockMultipartFile("image", "avatar.png", "image/png", imageBytes);

        String responseBody = mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String avatarUrl = objectMapper.readTree(responseBody).path("data").path("avatarUrl").asText();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").value(avatarUrl));

        String relativePath = avatarUrl.replaceFirst("^https?://[^/]+", "");
        mockMvc.perform(get(relativePath))
                .andExpect(status().isOk())
                .andExpect(content -> assertThat(content.getResponse().getContentAsByteArray())
                        .isEqualTo(imageBytes));
    }

    @Test
    @DisplayName("아바타를 다시 업로드하면 이전 파일은 스토리지에서 삭제된다")
    void uploadAvatar_replacingExisting_deletesPreviousFile() throws Exception {
        String accessToken = signUp();
        MockMultipartFile first = new MockMultipartFile("image", "first.png", "image/png", "first-bytes".getBytes());
        String firstResponse = mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(first)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String firstAvatarUrl = objectMapper.readTree(firstResponse).path("data").path("avatarUrl").asText();
        String firstRelativePath = firstAvatarUrl.replaceFirst("^https?://[^/]+", "");

        MockMultipartFile second = new MockMultipartFile("image", "second.png", "image/png", "second-bytes".getBytes());
        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(second)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get(firstRelativePath))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("서버 멀티파트 한도(10MB)를 초과하면 500이 아닌 400 에러가 발생한다")
    void uploadAvatar_exceedsServerMultipartLimit_returnsBadRequestNotServerError() throws Exception {
        String accessToken = signUp();
        byte[] overServerLimit = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile image = new MockMultipartFile("image", "avatar.png", "image/png", overServerLimit);

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("G400"));
    }

    @Test
    @DisplayName("이미지가 아닌 파일을 업로드하면 400 에러가 발생한다")
    void uploadAvatar_nonImageContentType_returnsBadRequest() throws Exception {
        String accessToken = signUp();
        MockMultipartFile file = new MockMultipartFile("image", "resume.pdf", "application/pdf", "not-an-image".getBytes());

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("G400"));
    }

    @Test
    @DisplayName("5MB를 초과하는 이미지를 업로드하면 400 에러가 발생한다")
    void uploadAvatar_overSizeLimit_returnsBadRequest() throws Exception {
        String accessToken = signUp();
        byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile image = new MockMultipartFile("image", "avatar.png", "image/png", tooLarge);

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("G400"));
    }

    @Test
    @DisplayName("인증 없이 업로드를 요청하면 401 에러가 발생한다")
    void uploadAvatar_withoutAuthentication_returnsUnauthorized() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "avatar.png", "image/png", "fake-png-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/users/me/avatar").file(image))
                .andExpect(status().isUnauthorized());
    }

    private String signUp() throws Exception {
        String phone = "010" + String.format("%08d", PHONE_SEQUENCE.incrementAndGet());

        mockMvc.perform(post("/api/v1/auth/signup/phone/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", phone))))
                .andExpect(status().isOk());

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
                        "nickname", "아바타테스트" + phone.substring(8),
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
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(signupBody).path("data");
        String accessToken = data.path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        return accessToken;
    }
}
