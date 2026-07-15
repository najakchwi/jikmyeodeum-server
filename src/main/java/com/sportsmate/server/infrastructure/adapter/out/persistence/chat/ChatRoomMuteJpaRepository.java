package com.sportsmate.server.infrastructure.adapter.out.persistence.chat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMuteJpaRepository extends JpaRepository<ChatRoomMuteEntity, ChatRoomMuteId> {
}
