package com.sportsmate.server.infrastructure.adapter.in.web.notification;

import com.sportsmate.server.domain.notification.port.in.NotificationUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Notification", description = "알림 목록, 읽음 처리, 알림 설정 API")
public class NotificationController {
    private final NotificationUseCase notificationUseCase;

    public NotificationController(NotificationUseCase notificationUseCase) {
        this.notificationUseCase = notificationUseCase;
    }
    @GetMapping("/notifications")
    @Operation(summary = "알림 목록 조회", description = "현재 회원의 알림 목록을 커서 기반으로 조회합니다.")
    public ApiResponse<NotificationUseCase.NotificationsResult> notifications(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "다음 페이지 조회 커서", example = "noti_123")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "조회할 알림 수", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notificationUseCase.notifications(
                Long.valueOf(memberId), cursor, size));
    }
    @GetMapping("/notifications/unread-count")
    @Operation(summary = "읽지 않은 알림 수 조회", description = "현재 회원의 미확인 알림 개수를 반환합니다.")
    public ApiResponse<UnreadCountResponse> unreadCount(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        return ApiResponse.success(new UnreadCountResponse(
                notificationUseCase.unreadCount(Long.valueOf(memberId))));
    }
    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "알림 읽음 처리", description = "지정한 알림 한 건을 읽음 상태로 변경합니다.")
    public ResponseEntity<Void> read(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "알림 ID", example = "noti_123")
            @PathVariable String id) {
        notificationUseCase.read(Long.valueOf(memberId), id);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/notifications/read-all")
    @Operation(summary = "모든 알림 읽음 처리", description = "현재 회원의 모든 알림을 읽음 상태로 변경합니다.")
    public ResponseEntity<Void> readAll(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        notificationUseCase.readAll(Long.valueOf(memberId));
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/notification-settings")
    @Operation(summary = "알림 설정 조회", description = "매칭, 일정, 채팅, 마케팅 알림 수신 설정을 조회합니다.")
    public ApiResponse<NotificationUseCase.SettingsResult> settings(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        return ApiResponse.success(notificationUseCase.settings(Long.valueOf(memberId)));
    }
    @PatchMapping("/notification-settings")
    @Operation(summary = "알림 설정 수정", description = "전달된 항목만 알림 수신 설정에 반영합니다.")
    public ApiResponse<NotificationUseCase.SettingsResult> updateSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @RequestBody NotificationUseCase.SettingsCommand command) {
        return ApiResponse.success(notificationUseCase.updateSettings(Long.valueOf(memberId), command));
    }
    @Schema(description = "읽지 않은 알림 수 응답")
    public record UnreadCountResponse(
            @Schema(description = "읽지 않은 알림 개수", example = "3")
            long count) {}
}
