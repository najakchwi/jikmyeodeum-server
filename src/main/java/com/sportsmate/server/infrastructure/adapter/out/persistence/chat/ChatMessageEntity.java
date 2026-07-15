package com.sportsmate.server.infrastructure.adapter.out.persistence.chat;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_messages")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "text", length = 1000, nullable = false)
    private String text;

    @Column(name = "type", length = 20, nullable = false)
    private String type;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
