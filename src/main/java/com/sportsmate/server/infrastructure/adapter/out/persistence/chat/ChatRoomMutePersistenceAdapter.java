package com.sportsmate.server.infrastructure.adapter.out.persistence.chat;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.chat.port.out.ChatRoomMuteOutPort;
import com.sportsmate.server.domain.notification.port.out.ChatMuteQueryPort;

@PersistenceAdapter
public class ChatRoomMutePersistenceAdapter implements ChatRoomMuteOutPort, ChatMuteQueryPort {
    private final ChatRoomMuteJpaRepository repository;

    public ChatRoomMutePersistenceAdapter(ChatRoomMuteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isMuted(Long memberId, String chatId) {
        return repository.existsById(new ChatRoomMuteId(memberId, chatId));
    }

    @Override
    public void mute(Long memberId, String chatId) {
        ChatRoomMuteId id = new ChatRoomMuteId(memberId, chatId);
        if (repository.existsById(id)) {
            return;
        }
        repository.save(ChatRoomMuteEntity.builder()
                .memberId(memberId)
                .chatId(chatId)
                .build());
    }

    @Override
    public void unmute(Long memberId, String chatId) {
        ChatRoomMuteId id = new ChatRoomMuteId(memberId, chatId);
        repository.findById(id).ifPresent(repository::delete);
    }
}
