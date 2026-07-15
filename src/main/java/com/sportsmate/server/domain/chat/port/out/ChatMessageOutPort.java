package com.sportsmate.server.domain.chat.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageOutPort {
    Message save(String chatId, Long senderId, String text, String type);
    List<Message> findMessages(String chatId, String cursor, int size);
    record Message(String id, String chatId, Long senderId, String text, LocalDateTime sentAt,
            String type) {}
}
