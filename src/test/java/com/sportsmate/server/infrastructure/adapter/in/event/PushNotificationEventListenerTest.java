package com.sportsmate.server.infrastructure.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.port.out.push.PushMessage;
import com.sportsmate.server.common.port.out.push.PushOutPort;
import com.sportsmate.server.domain.notification.event.PushNotificationRequestedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PushNotificationEventListener 단위 테스트")
class PushNotificationEventListenerTest {

    @Test
    @DisplayName("푸시 요청 이벤트를 받으면 PushOutPort로 발송한다")
    void handle_pushNotificationRequested_sendsPush() {
        RecordingPushOutPort pushOutPort = new RecordingPushOutPort();
        PushNotificationEventListener listener = new PushNotificationEventListener(pushOutPort);
        PushMessage message = new PushMessage(
                "ExponentPushToken[test]",
                "새 메시지가 도착했어요",
                "안녕하세요",
                Map.of("type", "chat"));

        listener.handle(new PushNotificationRequestedEvent(message));

        assertThat(pushOutPort.messages).containsExactly(message);
    }

    @Test
    @DisplayName("푸시 발송 실패는 이벤트 리스너 밖으로 전파하지 않는다")
    void handle_pushFailure_doesNotThrow() {
        PushNotificationEventListener listener = new PushNotificationEventListener(
                message -> { throw new IllegalStateException("failed"); });
        PushMessage message = new PushMessage(
                "ExponentPushToken[test]",
                "새 메시지가 도착했어요",
                "안녕하세요",
                Map.of("type", "chat"));

        listener.handle(new PushNotificationRequestedEvent(message));
    }

    private static class RecordingPushOutPort implements PushOutPort {
        private final List<PushMessage> messages = new ArrayList<>();

        @Override
        public void send(PushMessage message) {
            messages.add(message);
        }
    }
}
