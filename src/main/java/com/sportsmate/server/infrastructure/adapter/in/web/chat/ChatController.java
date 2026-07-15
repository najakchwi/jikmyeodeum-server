package com.sportsmate.server.infrastructure.adapter.in.web.chat;

import com.sportsmate.server.domain.chat.port.in.ChatUseCase;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chats")
@Tag(name = "Chat", description = "매칭 채팅방과 메시지 API")
public class ChatController {
    private final ChatUseCase chatUseCase;

    public ChatController(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }
    @GetMapping("/{chatId}")
    @Operation(summary = "채팅방 조회", description = "채팅방 기본 정보와 상대 프로필, 연결된 신청 정보를 조회합니다.")
    public ApiResponse<ChatUseCase.ChatRoomResult> room(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "채팅방 ID", example = "chat_123")
            @PathVariable String chatId) {
        return ApiResponse.success(chatUseCase.room(Long.valueOf(memberId), chatId));
    }
    @GetMapping("/{chatId}/messages")
    @Operation(summary = "채팅 메시지 목록 조회", description = "커서 기반으로 채팅방 메시지를 최신순 또는 서버 정책 순서대로 조회합니다.")
    public ApiResponse<ChatUseCase.MessagesResult> messages(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "채팅방 ID", example = "chat_123")
            @PathVariable String chatId,
            @Parameter(description = "다음 페이지 조회 커서", example = "msg_456")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "조회할 메시지 수", example = "30")
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.success(chatUseCase.messages(Long.valueOf(memberId), chatId, cursor, size));
    }
    @PostMapping("/{chatId}/messages")
    @Operation(summary = "채팅 메시지 전송", description = "채팅방에 새 텍스트 메시지를 전송합니다.")
    public ApiResponse<ChatUseCase.MessageResult> send(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "채팅방 ID", example = "chat_123")
            @PathVariable String chatId,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.created(chatUseCase.send(Long.valueOf(memberId), chatId, request.text()));
    }

    @PatchMapping("/{chatId}/notification-settings")
    @Operation(summary = "채팅방 알림 설정", description = "해당 채팅방의 푸시 알림을 켜거나 끕니다. 알림함 기록은 계속 남습니다.")
    public ApiResponse<NotificationSettingResponse> setNotificationEnabled(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Parameter(description = "채팅방 ID", example = "chat_123")
            @PathVariable String chatId,
            @Valid @RequestBody NotificationSettingRequest request) {
        boolean enabled = chatUseCase.setNotificationEnabled(
                Long.valueOf(memberId), chatId, request.enabled());
        return ApiResponse.success(new NotificationSettingResponse(enabled));
    }

    @Schema(description = "채팅 메시지 전송 요청")
    public record SendMessageRequest(
            @Schema(description = "전송할 메시지 본문. 최대 1000자", example = "안녕하세요! 경기장에서 뵐게요.")
            @NotBlank @Size(max = 1000) String text) {}

    @Schema(description = "채팅방 알림 설정 요청")
    public record NotificationSettingRequest(
            @Schema(description = "채팅방 푸시 알림 활성화 여부", example = "false")
            @NotNull Boolean enabled) {}

    @Schema(description = "채팅방 알림 설정 응답")
    public record NotificationSettingResponse(
            @Schema(description = "서버에 저장된 채팅방 푸시 알림 활성화 여부", example = "false")
            boolean enabled) {}
}
