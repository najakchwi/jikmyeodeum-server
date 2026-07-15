package com.sportsmate.server.domain.chat.service;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.chat.exception.ChatErrorCode;
import com.sportsmate.server.domain.chat.port.in.ChatUseCase;
import com.sportsmate.server.domain.chat.port.out.ChatMessageOutPort;
import com.sportsmate.server.domain.member.port.in.MemberUseCase;
import com.sportsmate.server.domain.notification.port.in.NotificationUseCase;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatService implements ChatUseCase {
    private final ApplicationOutPort applicationOutPort;
    private final ChatMessageOutPort chatMessageOutPort;
    private final MemberUseCase memberUseCase;
    private final NotificationUseCase notificationUseCase;

    public ChatService(ApplicationOutPort applicationOutPort, ChatMessageOutPort chatMessageOutPort,
            MemberUseCase memberUseCase, NotificationUseCase notificationUseCase) {
        this.applicationOutPort = applicationOutPort;
        this.chatMessageOutPort = chatMessageOutPort;
        this.memberUseCase = memberUseCase;
        this.notificationUseCase = notificationUseCase;
    }

    @Override
    public ChatRoomResult room(Long memberId, String chatId) {
        Application application = find(chatId, memberId);
        return new ChatRoomResult(
                chatId, application.getId(), memberUseCase.get(application.getMatchedMemberId()),
                "game_done".equals(application.getStatus()),
                "reviewed".equals(application.getStatus()),
                "cancelled".equals(application.getStatus()));
    }

    @Override
    public MessagesResult messages(Long memberId, String chatId, String cursor, int size) {
        find(chatId, memberId);
        List<ChatMessageOutPort.Message> messages = chatMessageOutPort.findMessages(
                chatId, cursor, Math.min(Math.max(size, 1), 100));
        List<MessageResult> results = new ArrayList<>();
        LocalDate previousDate = null;
        for (ChatMessageOutPort.Message message : messages) {
            LocalDate currentDate = message.sentAt().toLocalDate();
            LocalDate divider = currentDate.equals(previousDate) ? null : currentDate;
            results.add(toResult(message, divider));
            previousDate = currentDate;
        }
        String nextCursor = messages.size() < size ? null : messages.get(messages.size() - 1).id();
        return new MessagesResult(results, nextCursor);
    }

    @Override
    @Transactional
    public MessageResult send(Long memberId, String chatId, String text) {
        Application application = find(chatId, memberId);
        if ("game_done".equals(application.getStatus()) || "reviewed".equals(application.getStatus())
                || "cancelled".equals(application.getStatus())) {
            throw new BusinessException(ChatErrorCode.CHAT_CLOSED);
        }
        ChatMessageOutPort.Message message = chatMessageOutPort.save(chatId, memberId, text.trim(), "text");
        notificationUseCase.createAndPush(application.getMatchedMemberId(), "chat", "새 메시지가 도착했어요",
                text.trim(), "chat", null, chatId);
        return toResult(message, null);
    }

    @Override
    @Transactional
    public MessageResult postSystemMessage(String chatId, String text) {
        ChatMessageOutPort.Message message = chatMessageOutPort.save(chatId, null, text.trim(), "system");
        return toResult(message, null);
    }

    private Application find(String chatId, Long memberId) {
        return applicationOutPort.findByChatIdAndMemberId(chatId, memberId)
                .orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_NOT_FOUND));
    }

    private MessageResult toResult(ChatMessageOutPort.Message message, LocalDate divider) {
        return new MessageResult(
                message.id(), message.senderId(), message.text(),
                message.sentAt().format(DateTimeFormatter.ofPattern("HH:mm")), divider,
                message.type());
    }
}
