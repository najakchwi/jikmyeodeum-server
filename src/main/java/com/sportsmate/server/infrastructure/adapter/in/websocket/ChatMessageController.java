package com.sportsmate.server.infrastructure.adapter.in.websocket;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.ErrorCode;
import com.sportsmate.server.domain.chat.port.in.ChatUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class ChatMessageController {

    private final ChatUseCase chatUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageController(ChatUseCase chatUseCase, SimpMessagingTemplate messagingTemplate) {
        this.chatUseCase = chatUseCase;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chats/{chatId}/send")
    public void send(
            @DestinationVariable String chatId,
            @Valid SendMessagePayload payload,
            Principal principal) {
        try {
            chatUseCase.send(Long.valueOf(principal.getName()), chatId, payload.text());
        } catch (BusinessException e) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/errors", ChatErrorPayload.from(e));
        }
    }

    public record SendMessagePayload(@NotBlank @Size(max = 1000) String text) {}

    public record ChatErrorPayload(String errorCode, String message) {
        static ChatErrorPayload from(BusinessException exception) {
            ErrorCode errorCode = exception.getErrorCode();
            return new ChatErrorPayload(errorCode.getCode(), errorCode.getMessage());
        }
    }
}
