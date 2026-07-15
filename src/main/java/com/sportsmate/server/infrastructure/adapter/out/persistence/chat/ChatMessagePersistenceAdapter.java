package com.sportsmate.server.infrastructure.adapter.out.persistence.chat;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.chat.port.out.ChatMessageOutPort;
import java.util.List;

@PersistenceAdapter
public class ChatMessagePersistenceAdapter implements ChatMessageOutPort {
    private final ChatMessageJpaRepository repository;

    public ChatMessagePersistenceAdapter(ChatMessageJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Message save(String chatId, Long senderId, String text, String type) {
        ChatMessageEntity entity = ChatMessageEntity.builder()
                .matchId(Long.parseLong(chatId))
                .senderId(senderId)
                .text(text)
                .type(type)
                .sentAt(java.time.LocalDateTime.now())
                .build();
        return toMessage(repository.save(entity));
    }

    @Override
    public List<Message> findMessages(String chatId, String cursor, int size) {
        List<ChatMessageEntity> all = repository.findByMatchIdOrderBySentAtAsc(Long.parseLong(chatId));
        int start = 0;
        if (cursor != null) {
            for (int i = 0; i < all.size(); i++) {
                if (String.valueOf(all.get(i).getId()).equals(cursor)) {
                    start = i + 1;
                    break;
                }
            }
        }
        return all.stream().skip(start).limit(size).map(this::toMessage).toList();
    }

    private Message toMessage(ChatMessageEntity entity) {
        return new Message(String.valueOf(entity.getId()), String.valueOf(entity.getMatchId()), entity.getSenderId(),
                entity.getText(), entity.getSentAt(), entity.getType());
    }
}
