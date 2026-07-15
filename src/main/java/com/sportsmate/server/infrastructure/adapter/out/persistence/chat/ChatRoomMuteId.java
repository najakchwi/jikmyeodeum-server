package com.sportsmate.server.infrastructure.adapter.out.persistence.chat;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMuteId implements Serializable {
    private Long memberId;
    private String chatId;
}
