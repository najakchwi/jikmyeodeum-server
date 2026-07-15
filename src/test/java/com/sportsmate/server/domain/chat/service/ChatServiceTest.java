package com.sportsmate.server.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.chat.exception.ChatErrorCode;
import com.sportsmate.server.domain.chat.port.in.ChatUseCase;
import com.sportsmate.server.domain.chat.port.out.ChatBroadcastPort;
import com.sportsmate.server.domain.chat.port.out.ChatMessageOutPort;
import com.sportsmate.server.domain.chat.port.out.ChatRoomMuteOutPort;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import com.sportsmate.server.domain.member.port.in.MemberUseCase;
import com.sportsmate.server.domain.notification.port.in.NotificationUseCase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatService 단위 테스트")
class ChatServiceTest {

    private final FakeApplicationOutPort applicationOutPort = new FakeApplicationOutPort();
    private final FakeChatMessageOutPort chatMessageOutPort = new FakeChatMessageOutPort();
    private final FakeChatRoomMuteOutPort chatRoomMuteOutPort = new FakeChatRoomMuteOutPort();
    private final RecordingChatBroadcastPort chatBroadcastPort = new RecordingChatBroadcastPort();
    private final FakeMemberUseCase memberUseCase = new FakeMemberUseCase();
    private final RecordingNotificationUseCase notificationUseCase = new RecordingNotificationUseCase();
    private final ChatService chatService = new ChatService(
            applicationOutPort, chatMessageOutPort, chatRoomMuteOutPort, chatBroadcastPort, memberUseCase,
            notificationUseCase);

    @Test
    @DisplayName("메시지 전송 성공 시 저장된 메시지를 채팅 토픽으로 브로드캐스트한다")
    void send_success_broadcastsMessage() {
        applicationOutPort.save(application("app1", 1L, 2L, "chat1", "chatting"));

        ChatUseCase.MessageResult result = chatService.send(1L, "chat1", "안녕하세요");

        assertThat(result.text()).isEqualTo("안녕하세요");
        assertThat(chatBroadcastPort.records).containsExactly(new BroadcastRecord("chat1", result));
        assertThat(notificationUseCase.records)
                .extracting(NotificationRecord::memberId)
                .containsExactly(2L);
    }

    @Test
    @DisplayName("시스템 메시지 저장 성공 시 채팅 토픽으로 브로드캐스트한다")
    void postSystemMessage_success_broadcastsMessage() {
        ChatUseCase.MessageResult result = chatService.postSystemMessage("chat1", "member1님이 채팅에 참여했어요.");

        assertThat(result.type()).isEqualTo("system");
        assertThat(chatBroadcastPort.records).containsExactly(new BroadcastRecord("chat1", result));
    }

    @Test
    @DisplayName("닫힌 채팅에 메시지 전송 시 예외가 발생하고 브로드캐스트하지 않는다")
    void send_closedChat_throwsExceptionAndDoesNotBroadcast() {
        applicationOutPort.save(application("app1", 1L, 2L, "chat1", "game_done"));

        assertThatThrownBy(() -> chatService.send(1L, "chat1", "안녕하세요"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChatErrorCode.CHAT_CLOSED);
        assertThat(chatBroadcastPort.records).isEmpty();
    }

    @Test
    @DisplayName("브로드캐스트가 실패해도 메시지 전송은 성공하고 저장은 유지된다")
    void send_broadcastFails_returnsMessageAndKeepsSavedMessage() {
        applicationOutPort.save(application("app1", 1L, 2L, "chat1", "chatting"));
        ChatService service = new ChatService(applicationOutPort, chatMessageOutPort,
                chatRoomMuteOutPort, new FailingChatBroadcastPort(), memberUseCase, notificationUseCase);

        ChatUseCase.MessageResult result = service.send(1L, "chat1", "안녕하세요");

        assertThat(result.text()).isEqualTo("안녕하세요");
        assertThat(chatMessageOutPort.savedMessages)
                .extracting(ChatMessageOutPort.Message::text)
                .containsExactly("안녕하세요");
    }

    @Test
    @DisplayName("채팅 참가자이면 true를 반환한다")
    void isParticipant_existingParticipant_returnsTrue() {
        applicationOutPort.save(application("app1", 1L, 2L, "chat1", "chatting"));

        assertThat(chatService.isParticipant(1L, "chat1")).isTrue();
        assertThat(chatService.isParticipant(2L, "chat1")).isFalse();
    }

    @Test
    @DisplayName("채팅방 알림을 끄면 mute를 저장하고 false를 반환한다")
    void setNotificationEnabled_false_mutesChatRoom() {
        applicationOutPort.save(application("app1", 1L, 2L, "chat1", "chatting"));

        boolean result = chatService.setNotificationEnabled(1L, "chat1", false);
        boolean secondResult = chatService.setNotificationEnabled(1L, "chat1", false);

        assertThat(result).isFalse();
        assertThat(secondResult).isFalse();
        assertThat(chatRoomMuteOutPort.isMuted(1L, "chat1")).isTrue();
    }

    @Test
    @DisplayName("채팅방 알림을 켜면 mute를 제거하고 true를 반환한다")
    void setNotificationEnabled_true_unmutesChatRoom() {
        applicationOutPort.save(application("app1", 1L, 2L, "chat1", "chatting"));
        chatService.setNotificationEnabled(1L, "chat1", false);

        boolean result = chatService.setNotificationEnabled(1L, "chat1", true);

        assertThat(result).isTrue();
        assertThat(chatRoomMuteOutPort.isMuted(1L, "chat1")).isFalse();
    }

    @Test
    @DisplayName("참가자가 아닌 사용자가 채팅방 알림을 바꾸면 채팅방 조회와 같은 예외가 발생한다")
    void setNotificationEnabled_notParticipant_throwsChatNotFound() {
        applicationOutPort.save(application("app1", 1L, 2L, "chat1", "chatting"));

        assertThatThrownBy(() -> chatService.setNotificationEnabled(2L, "chat1", false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChatErrorCode.CHAT_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅방 조회 시 mute 상태를 알림 활성화 여부로 반환한다")
    void room_withMutedChat_returnsNotificationDisabled() {
        applicationOutPort.save(application("app1", 1L, 2L, "chat1", "chatting"));
        chatRoomMuteOutPort.mute(1L, "chat1");

        ChatUseCase.ChatRoomResult result = chatService.room(1L, "chat1");

        assertThat(result.notificationEnabled()).isFalse();
    }

    private static Application application(String id, Long memberId, Long matchedMemberId, String chatId,
            String status) {
        return Application.reconstitute(id, memberId, "game1", status, LocalDateTime.now(),
                matchedMemberId, chatId, LocalDateTime.now(), null, "accepted", null, 90, null);
    }

    private static class FakeApplicationOutPort implements ApplicationOutPort {
        private final Map<String, Application> applications = new LinkedHashMap<>();

        @Override public Application save(Application application) {
            applications.put(application.getId(), application);
            return application;
        }
        @Override public String createMatch(Application application, Application opponent) {
            throw new UnsupportedOperationException();
        }
        @Override public String createSoloMatch(Application application) {
            throw new UnsupportedOperationException();
        }
        @Override public void addParticipant(String chatId, Application application) {
            throw new UnsupportedOperationException();
        }
        @Override public Optional<Application> findByIdAndMemberId(String id, Long memberId) {
            return Optional.empty();
        }
        @Override public Optional<Application> findByMemberIdAndGameId(Long memberId, String gameId) {
            return Optional.empty();
        }
        @Override public Optional<Application> findByChatIdAndMemberId(String chatId, Long memberId) {
            return applications.values().stream()
                    .filter(application -> chatId.equals(application.getChatId()))
                    .filter(application -> memberId.equals(application.getMemberId()))
                    .findFirst();
        }
        @Override public List<Application> findByMemberId(Long memberId) {
            return List.of();
        }
        @Override public List<Application> findWaitingByGameId(String gameId) {
            return List.of();
        }
        @Override public List<String> findGameIdsWithWaitingApplications() {
            return List.of();
        }
        @Override public boolean existsActiveByMemberIdAndGameId(Long memberId, String gameId) {
            return false;
        }
        @Override public List<LocalDate> findAppliedDates(Long memberId, int year, int month) {
            return List.of();
        }
        @Override public long countChattingCancellationsSince(Long memberId, LocalDateTime since) {
            return 0;
        }
    }

    private static class FakeChatMessageOutPort implements ChatMessageOutPort {
        private final List<Message> savedMessages = new ArrayList<>();
        private int sequence = 1;

        @Override
        public Message save(String chatId, Long senderId, String text, String type) {
            Message message = new Message("msg" + sequence++, chatId, senderId, text,
                    LocalDateTime.of(2026, 7, 15, 14, 3), type);
            savedMessages.add(message);
            return message;
        }

        @Override public List<Message> findMessages(String chatId, String cursor, int size) {
            return List.of();
        }
    }

    private static class FakeChatRoomMuteOutPort implements ChatRoomMuteOutPort {
        private final Map<Long, List<String>> mutes = new LinkedHashMap<>();

        @Override
        public boolean isMuted(Long memberId, String chatId) {
            return mutes.getOrDefault(memberId, List.of()).contains(chatId);
        }

        @Override
        public void mute(Long memberId, String chatId) {
            mutes.computeIfAbsent(memberId, ignored -> new ArrayList<>());
            if (!mutes.get(memberId).contains(chatId)) {
                mutes.get(memberId).add(chatId);
            }
        }

        @Override
        public void unmute(Long memberId, String chatId) {
            List<String> memberMutes = mutes.get(memberId);
            if (memberMutes != null) {
                memberMutes.remove(chatId);
            }
        }
    }

    private static class RecordingChatBroadcastPort implements ChatBroadcastPort {
        private final List<BroadcastRecord> records = new ArrayList<>();

        @Override
        public void broadcast(String chatId, ChatUseCase.MessageResult message) {
            records.add(new BroadcastRecord(chatId, message));
        }
    }

    private static class FailingChatBroadcastPort implements ChatBroadcastPort {
        @Override
        public void broadcast(String chatId, ChatUseCase.MessageResult message) {
            throw new IllegalStateException("broker unavailable");
        }
    }

    private record BroadcastRecord(String chatId, ChatUseCase.MessageResult message) {}

    private static class FakeMemberUseCase implements MemberUseCase {
        @Override public MemberProfile get(Long memberId) {
            return new MemberProfile(memberId, "01012345678", "phone", "member" + memberId, null,
                    LocalDate.of(1995, 1, 1), Gender.MALE.name().toLowerCase(), null, "#FFFFFF", "LG",
                    0, 0.0, 80, List.of(WatchStyle.FOCUS), Personality.CALM, TalkStyle.QUIET,
                    SmokingStatus.NON_SMOKER, GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5,
                    true, "서울시 송파구", 37.0, 127.0, 0, 0, null);
        }
        @Override public MemberProfile updateProfile(Long memberId, String nickname, String bio,
                LocalDate birthdate, Gender gender, String team) {
            throw new UnsupportedOperationException();
        }
        @Override public MemberProfile updateStyle(Long memberId, String team, List<WatchStyle> watchStyles,
                Personality personality, TalkStyle talkStyle, SmokingStatus smokingStatus) {
            throw new UnsupportedOperationException();
        }
        @Override public MemberProfile updatePreference(Long memberId, GenderPref genderPref, AgePref agePref,
                SmokingPref smokingPref, Integer distanceKm) {
            throw new UnsupportedOperationException();
        }
        @Override public LocationVerifyResult verifyLocation(Long memberId, double latitude, double longitude) {
            throw new UnsupportedOperationException();
        }
        @Override public MemberProfile updateAvatar(Long memberId, String avatarUrl) {
            throw new UnsupportedOperationException();
        }
        @Override public TrustScoreResult getTrustScore(Long memberId) {
            throw new UnsupportedOperationException();
        }
    }

    private static class RecordingNotificationUseCase implements NotificationUseCase {
        private final List<NotificationRecord> records = new ArrayList<>();

        @Override public NotificationsResult notifications(Long memberId, String cursor, int size) {
            throw new UnsupportedOperationException();
        }
        @Override public long unreadCount(Long memberId) {
            throw new UnsupportedOperationException();
        }
        @Override public void read(Long memberId, String notificationId) {
            throw new UnsupportedOperationException();
        }
        @Override public void readAll(Long memberId) {
            throw new UnsupportedOperationException();
        }
        @Override public SettingsResult settings(Long memberId) {
            throw new UnsupportedOperationException();
        }
        @Override public SettingsResult updateSettings(Long memberId, SettingsCommand command) {
            throw new UnsupportedOperationException();
        }
        @Override public void createAndPush(Long memberId, String type, String title, String body,
                String targetKind, String applicationId, String chatId) {
            records.add(new NotificationRecord(memberId, type, title, body, targetKind, applicationId, chatId));
        }
    }

    private record NotificationRecord(Long memberId, String type, String title, String body,
            String targetKind, String applicationId, String chatId) {}
}
