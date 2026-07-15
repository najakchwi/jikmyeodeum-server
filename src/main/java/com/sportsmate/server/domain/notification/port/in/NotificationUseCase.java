package com.sportsmate.server.domain.notification.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationUseCase {
    NotificationsResult notifications(Long memberId, String cursor, int size);
    long unreadCount(Long memberId);
    void read(Long memberId, String notificationId);
    void readAll(Long memberId);
    SettingsResult settings(Long memberId);
    SettingsResult updateSettings(Long memberId, SettingsCommand command);
    void createAndPush(Long memberId, String type, String title, String body,
            String targetKind, String applicationId, String chatId);

    record NotificationsResult(List<NotificationResult> notifications, String nextCursor) {}
    record NotificationResult(String id, String type, String title, String body,
            LocalDateTime createdAt, boolean read, Target target) {}
    record Target(String kind, String applicationId, String chatId) {}
    record SettingsResult(boolean matchRequest, boolean matchSchedule, boolean chat,
            boolean review, boolean marketing) {}
    record SettingsCommand(Boolean matchRequest, Boolean matchSchedule, Boolean chat,
            Boolean review, Boolean marketing) {}
}
