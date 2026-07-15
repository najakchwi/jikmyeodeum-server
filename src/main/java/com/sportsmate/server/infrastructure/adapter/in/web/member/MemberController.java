package com.sportsmate.server.infrastructure.adapter.in.web.member;

import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import com.sportsmate.server.domain.member.port.in.MemberUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.common.port.out.storage.StoredObject;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Member", description = "내 프로필과 매칭 기준 관리 API")
public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    private final MemberUseCase memberUseCase;
    private final ObjectStorage objectStorage;

    public MemberController(MemberUseCase memberUseCase, ObjectStorage objectStorage) {
        this.memberUseCase = memberUseCase;
        this.objectStorage = objectStorage;
    }

    @GetMapping
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 회원의 기본 프로필, 응원팀, 관람 성향, 선호 조건을 조회합니다.")
    public ApiResponse<MemberProfile> get(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        return ApiResponse.success(memberUseCase.get(Long.valueOf(memberId)));
    }

    @PatchMapping({"", "/profile"})
    @Operation(summary = "내 기본 프로필 수정", description = "닉네임, 소개, 생년월일, 성별, 응원팀 등 기본 프로필 정보를 수정합니다.")
    public ApiResponse<MemberProfile> update(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(memberUseCase.updateProfile(
                Long.valueOf(memberId), request.nickname(), request.bio(), request.birthdate(),
                request.gender(), request.team()));
    }

    @PatchMapping("/style")
    @Operation(summary = "내 관람 성향 수정", description = "응원팀, 관람 스타일, 성격, 대화 스타일, 흡연 여부를 수정합니다.")
    public ApiResponse<MemberProfile> updateStyle(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody UpdateStyleRequest request) {
        return ApiResponse.success(memberUseCase.updateStyle(
                Long.valueOf(memberId), request.team(), request.watchStyles(),
                request.personality(), request.talkStyle(), request.smokingStatus()));
    }

    @PatchMapping({"/preference", "/companion-preference"})
    @Operation(summary = "동행 선호 조건 수정", description = "동행 상대의 성별, 연령대, 흡연 여부, 매칭 허용 거리를 수정합니다.")
    public ApiResponse<MemberProfile> updatePreference(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody UpdatePreferenceRequest request) {
        return ApiResponse.success(memberUseCase.updatePreference(
                Long.valueOf(memberId), request.genderPref(), request.agePref(),
                request.smokingPref(), request.distanceKm()));
    }

    @PostMapping("/location")
    @Operation(summary = "활동 위치 인증", description = "현재 위치 좌표를 검증하고 행정구역 정보와 위치 인증 상태를 저장합니다.")
    public ApiResponse<MemberUseCase.LocationVerifyResult> verifyLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody LocationRequest request) {
        return ApiResponse.success(memberUseCase.verifyLocation(
                Long.valueOf(memberId), request.latitude(), request.longitude()));
    }

    @GetMapping("/trust-score")
    @Operation(summary = "신뢰 점수 조회", description = "현재 회원의 신뢰 점수, 등급, 산정 기준을 조회합니다.")
    public ApiResponse<MemberUseCase.TrustScoreResult> trustScore(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        return ApiResponse.success(memberUseCase.getTrustScore(Long.valueOf(memberId)));
    }

    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @Operation(
            summary = "아바타 이미지 업로드",
            description = "5MB 이하 이미지 파일을 업로드하고 내 프로필 아바타 URL을 갱신합니다.")
    public ApiResponse<AvatarResponse> uploadAvatar(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "업로드할 이미지 파일. image/* MIME 타입, 최대 5MB")
            @RequestPart("image") MultipartFile image) throws IOException {
        if (image.isEmpty() || image.getContentType() == null
                || !image.getContentType().startsWith("image/")
                || image.getSize() > 5 * 1024 * 1024) {
            throw new com.sportsmate.server.common.exception.BusinessException(
                    com.sportsmate.server.common.exception.CommonErrorCode.INVALID_INPUT);
        }
        String previousAvatarUrl = memberUseCase.get(Long.valueOf(memberId)).avatarUrl();
        String extension = image.getOriginalFilename() != null
                && image.getOriginalFilename().contains(".")
                ? image.getOriginalFilename().substring(image.getOriginalFilename().lastIndexOf('.'))
                : "";
        var stored = upload(memberId, image, extension);
        memberUseCase.updateAvatar(Long.valueOf(memberId), stored.url());
        deletePreviousAvatar(previousAvatarUrl);
        return ApiResponse.success(new AvatarResponse(stored.url()));
    }

    private StoredObject upload(
            String memberId, MultipartFile image, String extension) throws IOException {
        try (InputStream inputStream = image.getInputStream()) {
            return objectStorage.upload(new ObjectUploadCommand(
                    "images/profile-avatar/" + memberId + "/" + UUID.randomUUID() + extension,
                    image.getContentType(), image.getSize(), inputStream));
        }
    }

    private void deletePreviousAvatar(String previousAvatarUrl) {
        if (previousAvatarUrl == null || previousAvatarUrl.isBlank()) return;
        String previousKey = objectStorage.extractKey(previousAvatarUrl);
        if (previousKey == null || previousKey.isBlank()) return;
        try {
            objectStorage.delete(previousKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete previous avatar: {}", previousKey, exception);
        }
    }

    @Schema(description = "내 기본 프로필 수정 요청")
    public record UpdateProfileRequest(
            @Schema(description = "닉네임. 2~12자", example = "야구친구")
            @Size(min = 2, max = 12) String nickname,
            @Schema(description = "한 줄 소개. 최대 50자", example = "잠실 직관 자주 갑니다", nullable = true)
            @Size(max = 50) String bio,
            @Schema(description = "생년월일", example = "1998-04-12", nullable = true)
            LocalDate birthdate,
            @Schema(description = "성별", example = "MALE", nullable = true)
            Gender gender,
            @Schema(description = "응원팀 이름", example = "LG 트윈스", nullable = true)
            String team) {}
    @Schema(description = "내 관람 성향 수정 요청")
    public record UpdateStyleRequest(
            @Schema(description = "응원팀 이름", example = "LG 트윈스", nullable = true)
            String team,
            @Schema(description = "관람 스타일. 최대 2개", example = "[\"cheer\", \"food\"]")
            @Size(max = 2) List<WatchStyle> watchStyles,
            @Schema(description = "본인 성격", example = "tension", nullable = true)
            Personality personality,
            @Schema(description = "대화 스타일", example = "talkative", nullable = true)
            TalkStyle talkStyle,
            @Schema(description = "흡연 여부", example = "non-smoker", nullable = true)
            SmokingStatus smokingStatus) {}
    @Schema(description = "동행 선호 조건 수정 요청")
    public record UpdatePreferenceRequest(
            @Schema(description = "선호 동행 성별", example = "any", nullable = true)
            GenderPref genderPref,
            @Schema(description = "선호 동행 연령대", example = "similar", nullable = true)
            AgePref agePref,
            @Schema(description = "선호 동행 흡연 여부", example = "non-smoker", nullable = true)
            SmokingPref smokingPref,
            @Schema(description = "매칭 허용 거리(km). 1~100", example = "10", nullable = true)
            @Min(1) @Max(100) Integer distanceKm) {}
    @Schema(description = "활동 위치 인증 요청")
    public record LocationRequest(
            @Schema(description = "위도", example = "37.5145")
            @DecimalMin("-90") @DecimalMax("90") double latitude,
            @Schema(description = "경도", example = "127.1059")
            @DecimalMin("-180") @DecimalMax("180") double longitude) {}
    @Schema(description = "아바타 업로드 응답")
    public record AvatarResponse(
            @Schema(description = "업로드된 아바타 이미지 URL", example = "https://cdn.example.com/avatars/1/profile.png")
            String avatarUrl) {}
}
