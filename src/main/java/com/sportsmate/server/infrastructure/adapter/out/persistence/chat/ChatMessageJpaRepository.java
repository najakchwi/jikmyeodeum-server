package com.sportsmate.server.infrastructure.adapter.out.persistence.chat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByMatchIdOrderBySentAtAsc(Long matchId);
}
