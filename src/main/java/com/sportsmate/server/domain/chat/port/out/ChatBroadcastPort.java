package com.sportsmate.server.domain.chat.port.out;

import com.sportsmate.server.domain.chat.port.in.ChatUseCase;

public interface ChatBroadcastPort {
    void broadcast(String chatId, ChatUseCase.MessageResult message);
}
