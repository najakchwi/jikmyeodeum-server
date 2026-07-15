package com.sportsmate.server.domain.notification.service;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.common.port.out.event.EventPublisher;
import com.sportsmate.server.common.port.out.push.PushMessage;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.notification.event.PushNotificationRequestedEvent;
import com.sportsmate.server.domain.notification.port.in.NotificationUseCase;
import com.sportsmate.server.domain.notification.port.in.PushTokenUseCase;
import com.sportsmate.server.domain.notification.port.out.NotificationOutPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService implements NotificationUseCase, PushTokenUseCase {
    private final NotificationOutPort notificationOutPort;
    private final MemberOutPort memberOutPort;
    private final EventPublisher eventPublisher;

    public NotificationService(NotificationOutPort notificationOutPort, MemberOutPort memberOutPort,
            EventPublisher eventPublisher) {
        this.notificationOutPort = notificationOutPort;
        this.memberOutPort = memberOutPort;
        this.eventPublisher = eventPublisher;
    }
    @Override
    public NotificationsResult notifications(Long memberId, String cursor, int size) {
        List<NotificationOutPort.NotificationData> data = notificationOutPort.findByMemberId(
                memberId, cursor, Math.min(Math.max(size, 1), 100));
        List<NotificationResult> results = data.stream().map(this::toResult).toList();
        String nextCursor = data.size() < size ? null : data.get(data.size() - 1).id();
        return new NotificationsResult(results, nextCursor);
    }
    @Override public long unreadCount(Long memberId) { return notificationOutPort.countUnread(memberId); }
    @Override @Transactional public void read(Long memberId, String id) {
        notificationOutPort.findByIdAndMemberId(id, memberId).ifPresent(data -> notificationOutPort.markRead(id));
    }
    @Override @Transactional public void readAll(Long memberId) { notificationOutPort.markAllRead(memberId); }
    @Override public SettingsResult settings(Long memberId) {
        return toResult(notificationOutPort.getOrCreateSettings(memberId));
    }
    @Override
    @Transactional
    public SettingsResult updateSettings(Long memberId, SettingsCommand command) {
        NotificationOutPort.SettingsData current = notificationOutPort.getOrCreateSettings(memberId);
        return toResult(notificationOutPort.saveSettings(new NotificationOutPort.SettingsData(
                memberId,
                command.matchRequest() == null ? current.matchRequest() : command.matchRequest(),
                command.matchSchedule() == null ? current.matchSchedule() : command.matchSchedule(),
                command.chat() == null ? current.chat() : command.chat(),
                command.review() == null ? current.review() : command.review(),
                command.marketing() == null ? current.marketing() : command.marketing())));
    }
    @Override
    @Transactional
    public void createAndPush(Long memberId, String type, String title, String body,
            String targetKind, String applicationId, String chatId) {
        notificationOutPort.create(memberId, type, title, body, targetKind, applicationId, chatId);
        if (!pushAllowed(memberId, type)) {
            return;
        }
        memberOutPort.findExpoPushTokenById(memberId)
                .filter(token -> !token.isBlank())
                .ifPresent(token -> eventPublisher.publish(new PushNotificationRequestedEvent(
                        new PushMessage(token, title, body,
                                pushData(type, targetKind, applicationId, chatId)))));
    }

    @Override
    @Transactional
    public void register(Long memberId, String token) {
        if (!validExpoPushToken(token)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        memberOutPort.updateExpoPushToken(memberId, token);
        if (memberOutPort.markWelcomeNotified(memberId)) {
            createAndPush(memberId, "system", "직며듦에 오신 것을 환영해요!",
                    "응원하는 경기의 직관 동행을 찾아보세요.", "findMatch", null, null);
        }
    }

    @Override
    @Transactional
    public void delete(Long memberId) {
        memberOutPort.updateExpoPushToken(memberId, null);
    }

    private NotificationResult toResult(NotificationOutPort.NotificationData data) {
        Target target = data.targetKind() == null ? null
                : new Target(data.targetKind(), data.applicationId(), data.chatId());
        return new NotificationResult(data.id(), data.type(), data.title(), data.body(),
                data.createdAt(), data.read(), target);
    }
    private SettingsResult toResult(NotificationOutPort.SettingsData data) {
        return new SettingsResult(data.matchRequest(), data.matchSchedule(), data.chat(),
                data.review(), data.marketing());
    }

    private boolean pushAllowed(Long memberId, String type) {
        NotificationOutPort.SettingsData settings = notificationOutPort.getOrCreateSettings(memberId);
        return switch (type) {
            case "match" -> settings.matchRequest();
            case "matchSchedule" -> settings.matchSchedule();
            case "chat" -> settings.chat();
            case "review" -> settings.review();
            case "marketing" -> settings.marketing();
            default -> true;
        };
    }

    private Map<String, String> pushData(String type, String targetKind, String applicationId,
            String chatId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", type);
        if (targetKind != null) data.put("targetKind", targetKind);
        if (applicationId != null) data.put("applicationId", applicationId);
        if (chatId != null) data.put("chatId", chatId);
        return data;
    }

    private boolean validExpoPushToken(String token) {
        return token != null
                && (token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken["))
                && token.endsWith("]");
    }
}
