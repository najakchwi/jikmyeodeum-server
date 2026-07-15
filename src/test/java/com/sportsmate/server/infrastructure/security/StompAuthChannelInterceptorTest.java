package com.sportsmate.server.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.domain.chat.port.in.ChatUseCase;
import com.sportsmate.server.infrastructure.adapter.out.token.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@DisplayName("StompAuthChannelInterceptor 단위 테스트")
class StompAuthChannelInterceptorTest {

    private static final String SECRET = "12345678901234567890123456789012";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3600L, 7200L);
    private final FakeChatUseCase chatUseCase = new FakeChatUseCase();
    private final StompAuthChannelInterceptor interceptor =
            new StompAuthChannelInterceptor(jwtProvider, chatUseCase);

    @Test
    @DisplayName("유효한 Bearer 토큰으로 CONNECT 시 Principal을 세팅한다")
    void connect_validToken_setsPrincipal() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + jwtProvider.issue("1", Role.USER).accessToken());
        accessor.setLeaveMutable(true);

        interceptor.preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("1");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 CONNECT를 거부한다")
    void connect_missingToken_throwsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        assertThatThrownBy(() ->
                interceptor.preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("채팅 참가자가 같은 채팅 토픽을 구독하면 허용한다")
    void subscribe_participant_allowsSubscription() {
        chatUseCase.participants.add("1:chat1");
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chats/chat1");
        accessor.setUser(new UsernamePasswordAuthenticationToken("1", null));

        var result = interceptor.preSend(
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("채팅 참가자가 아니면 채팅 토픽 구독을 거부한다")
    void subscribe_nonParticipant_throwsException() {
        chatUseCase.participants.add("1:chat1");
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chats/chat1");
        accessor.setUser(new UsernamePasswordAuthenticationToken("2", null));

        assertThatThrownBy(() ->
                interceptor.preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null))
                .isInstanceOf(MessagingException.class);
    }

    private static class FakeChatUseCase implements ChatUseCase {
        private final List<String> participants = new java.util.ArrayList<>();

        @Override public ChatRoomResult room(Long memberId, String chatId) {
            throw new UnsupportedOperationException();
        }
        @Override public MessagesResult messages(Long memberId, String chatId, String cursor, int size) {
            throw new UnsupportedOperationException();
        }
        @Override public MessageResult send(Long memberId, String chatId, String text) {
            throw new UnsupportedOperationException();
        }
        @Override public MessageResult postSystemMessage(String chatId, String text) {
            throw new UnsupportedOperationException();
        }
        @Override public boolean isParticipant(Long memberId, String chatId) {
            return participants.contains(memberId + ":" + chatId);
        }
        @Override public boolean setNotificationEnabled(Long memberId, String chatId, boolean enabled) {
            throw new UnsupportedOperationException();
        }
    }
}
