package com.sportsmate.server.infrastructure.adapter.out.websocket;

import com.sportsmate.server.domain.chat.port.in.ChatUseCase;
import com.sportsmate.server.domain.chat.port.out.ChatBroadcastPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatBroadcastAdapter implements ChatBroadcastPort {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatBroadcastAdapter(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcast(String chatId, ChatUseCase.MessageResult message) {
        messagingTemplate.convertAndSend("/topic/chats/" + chatId, message);
    }
}
