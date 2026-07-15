package com.sportsmate.server.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.domain.Event;
import com.sportsmate.server.common.port.out.event.EventPublisher;
import com.sportsmate.server.common.port.out.push.PushMessage;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.notification.event.PushNotificationRequestedEvent;
import com.sportsmate.server.domain.notification.port.out.NotificationOutPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {

    private final FakeNotificationOutPort notificationOutPort = new FakeNotificationOutPort();
    private final FakeMemberOutPort memberOutPort = new FakeMemberOutPort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final NotificationService notificationService = new NotificationService(
            notificationOutPort, memberOutPort, eventPublisher);

    @Test
    @DisplayName("알림 생성 시 설정이 허용되어 있고 토큰이 있으면 푸시 요청 이벤트를 발행한다")
    void createAndPush_withAllowedSettingsAndToken_publishesPushEvent() {
        memberOutPort.updateExpoPushToken(1L, "ExponentPushToken[test]");

        notificationService.createAndPush(1L, "chat", "새 메시지가 도착했어요",
                "안녕하세요", "chat", null, "chat_1");

        assertThat(notificationOutPort.notifications).hasSize(1);
        assertThat(eventPublisher.events).hasSize(1);
        PushMessage message = ((PushNotificationRequestedEvent) eventPublisher.events.get(0)).getMessage();
        assertThat(message.to()).isEqualTo("ExponentPushToken[test]");
        assertThat(message.data()).containsEntry("type", "chat")
                .containsEntry("targetKind", "chat")
                .containsEntry("chatId", "chat_1");
    }

    @Test
    @DisplayName("알림 설정이 꺼져 있으면 DB 알림만 저장하고 푸시는 발송하지 않는다")
    void createAndPush_withDisabledSettings_doesNotSendPush() {
        memberOutPort.updateExpoPushToken(1L, "ExponentPushToken[test]");
        notificationOutPort.settings = new NotificationOutPort.SettingsData(
                1L, true, true, false, true, false);

        notificationService.createAndPush(1L, "chat", "새 메시지가 도착했어요",
                "안녕하세요", "chat", null, "chat_1");

        assertThat(notificationOutPort.notifications).hasSize(1);
        assertThat(eventPublisher.events).isEmpty();
    }

    @Test
    @DisplayName("푸시 토큰 등록 시 첫 등록이면 환영 알림을 발송한다")
    void register_withFirstPushToken_createsWelcomeNotificationAndPublishesPushEvent() {
        notificationService.register(1L, "ExpoPushToken[test]");

        assertThat(memberOutPort.findExpoPushTokenById(1L)).contains("ExpoPushToken[test]");
        assertThat(memberOutPort.isWelcomeNotified(1L)).isTrue();
        assertThat(notificationOutPort.notifications).hasSize(1);
        assertThat(eventPublisher.events).hasSize(1);
        PushMessage message = ((PushNotificationRequestedEvent) eventPublisher.events.get(0)).getMessage();
        assertThat(message.to()).isEqualTo("ExpoPushToken[test]");
        assertThat(message.title()).isEqualTo("직며듦에 오신 것을 환영해요!");
        assertThat(message.data()).containsEntry("type", "system")
                .containsEntry("targetKind", "findMatch");
    }

    @Test
    @DisplayName("푸시 토큰을 다시 등록해도 환영 알림은 중복 발송하지 않는다")
    void register_withAlreadyWelcomeNotified_doesNotSendWelcomeAgain() {
        notificationService.register(1L, "ExpoPushToken[first]");
        notificationService.delete(1L);
        notificationService.register(1L, "ExpoPushToken[second]");

        assertThat(memberOutPort.findExpoPushTokenById(1L)).contains("ExpoPushToken[second]");
        assertThat(notificationOutPort.notifications).hasSize(1);
        assertThat(eventPublisher.events).hasSize(1);
    }

    @Test
    @DisplayName("푸시 토큰 삭제를 회원 저장소에 위임한다")
    void deletePushToken_updatesMemberToken() {
        notificationService.register(1L, "ExpoPushToken[test]");

        notificationService.delete(1L);

        assertThat(memberOutPort.findExpoPushTokenById(1L)).isEmpty();
    }

    private static class FakeNotificationOutPort implements NotificationOutPort {
        private final List<NotificationData> notifications = new ArrayList<>();
        private SettingsData settings = new SettingsData(1L, true, true, true, true, false);

        @Override
        public List<NotificationData> findByMemberId(Long memberId, String cursor, int size) {
            return notifications;
        }

        @Override
        public long countUnread(Long memberId) {
            return notifications.stream().filter(notification -> !notification.read()).count();
        }

        @Override
        public Optional<NotificationData> findByIdAndMemberId(String id, Long memberId) {
            return notifications.stream()
                    .filter(notification -> notification.id().equals(id))
                    .filter(notification -> notification.memberId().equals(memberId))
                    .findFirst();
        }

        @Override
        public void markRead(String id) {
        }

        @Override
        public void markAllRead(Long memberId) {
        }

        @Override
        public SettingsData getOrCreateSettings(Long memberId) {
            return settings;
        }

        @Override
        public SettingsData saveSettings(SettingsData settings) {
            this.settings = settings;
            return settings;
        }

        @Override
        public void create(Long memberId, String type, String title, String body, String targetKind,
                String applicationId, String chatId) {
            notifications.add(new NotificationData(
                    String.valueOf(notifications.size() + 1), memberId, type, title, body,
                    LocalDateTime.now(), false, targetKind, applicationId, chatId));
        }
    }

    private static class FakeMemberOutPort implements MemberOutPort {
        private String expoPushToken;
        private boolean welcomeNotified;

        @Override
        public Member save(Member member) {
            return member;
        }

        @Override
        public Optional<Member> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<Member> findByPhone(String phone) {
            return Optional.empty();
        }

        @Override
        public Optional<Member> findByProvider(LoginType loginType, String providerId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findExpoPushTokenById(Long id) {
            return Optional.ofNullable(expoPushToken);
        }

        @Override
        public boolean isWelcomeNotified(Long id) {
            return welcomeNotified;
        }

        @Override
        public boolean existsByPhone(String phone) {
            return false;
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return false;
        }

        @Override
        public void updateExpoPushToken(Long id, String expoPushToken) {
            this.expoPushToken = expoPushToken;
        }

        @Override
        public boolean markWelcomeNotified(Long id) {
            if (welcomeNotified) {
                return false;
            }
            welcomeNotified = true;
            return true;
        }

        @Override
        public void withdraw(Long id) {
        }
    }

    private static class RecordingEventPublisher implements EventPublisher {
        private final List<Event> events = new ArrayList<>();

        @Override
        public void publish(Event event) {
            events.add(event);
        }
    }
}
