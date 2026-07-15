package com.sportsmate.server.infrastructure.security;

import com.sportsmate.server.domain.chat.port.in.ChatUseCase;
import com.sportsmate.server.infrastructure.adapter.out.token.JwtProvider;
import java.security.Principal;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CHAT_TOPIC_PREFIX = "/topic/chats/";

    private final JwtProvider jwtProvider;
    private final ChatUseCase chatUseCase;

    public StompAuthChannelInterceptor(JwtProvider jwtProvider, @Lazy ChatUseCase chatUseCase) {
        this.jwtProvider = jwtProvider;
        this.chatUseCase = chatUseCase;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = extractBearerToken(accessor.getFirstNativeHeader("Authorization"));
        Authentication authentication = token == null ? null : jwtProvider.toAuthentication(token).orElse(null);
        if (authentication == null) {
            throw new MessagingException("Invalid STOMP authorization");
        }

        accessor.setUser(authentication);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
            return;
        }

        Principal user = accessor.getUser();
        if (user == null) {
            throw new MessagingException("Unauthenticated STOMP subscription");
        }

        String chatId = destination.substring(CHAT_TOPIC_PREFIX.length());
        if (chatId.isBlank()) {
            throw new MessagingException("Invalid chat topic");
        }

        Long memberId = Long.valueOf(user.getName());
        if (!chatUseCase.isParticipant(memberId, chatId)) {
            throw new MessagingException("Forbidden chat topic");
        }
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
