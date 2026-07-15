package com.sportsmate.server.domain.chat.port.in;

import com.sportsmate.server.domain.member.port.in.MemberProfile;
import java.time.LocalDate;
import java.util.List;

public interface ChatUseCase {
    ChatRoomResult room(Long memberId, String chatId);
    MessagesResult messages(Long memberId, String chatId, String cursor, int size);
    MessageResult send(Long memberId, String chatId, String text);
    MessageResult postSystemMessage(String chatId, String text);

    record ChatRoomResult(String id, String applicationId, MemberProfile opponent,
            boolean isGameDone, boolean isReviewed, boolean isCancelled) {}
    record MessagesResult(List<MessageResult> messages, String nextCursor) {}
    record MessageResult(String id, Long senderId, String text, String sentAt, LocalDate date,
            String type) {}
}
