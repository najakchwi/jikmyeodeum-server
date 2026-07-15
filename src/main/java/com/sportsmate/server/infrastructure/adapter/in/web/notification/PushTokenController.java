package com.sportsmate.server.infrastructure.adapter.in.web.notification;

import com.sportsmate.server.domain.notification.port.in.PushTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/push-token")
@Tag(name = "Notification", description = "푸시 알림 토큰 관리 API")
public class PushTokenController {

    private final PushTokenUseCase pushTokenUseCase;

    public PushTokenController(PushTokenUseCase pushTokenUseCase) {
        this.pushTokenUseCase = pushTokenUseCase;
    }

    @PutMapping
    @Operation(summary = "푸시 토큰 등록", description = "Expo 또는 FCM 등 클라이언트 푸시 토큰을 현재 회원에 저장합니다.")
    public ResponseEntity<Void> register(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody PushTokenRequest request) {
        pushTokenUseCase.register(Long.valueOf(memberId), request.token());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "푸시 토큰 삭제", description = "현재 회원에 저장된 푸시 토큰을 삭제합니다.")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        pushTokenUseCase.delete(Long.valueOf(memberId));
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "푸시 토큰 등록 요청")
    public record PushTokenRequest(
            @Schema(description = "클라이언트 푸시 토큰", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
            @NotBlank String token) {
    }
}
