package com.sportsmate.server.domain.notification.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutPort {
    List<NotificationData> findByMemberId(Long memberId, String cursor, int size);
    long countUnread(Long memberId);
    Optional<NotificationData> findByIdAndMemberId(String id, Long memberId);
    void markRead(String id);
    void markAllRead(Long memberId);
    SettingsData getOrCreateSettings(Long memberId);
    SettingsData saveSettings(SettingsData settings);
    void create(Long memberId, String type, String title, String body, String targetKind,
            String applicationId, String chatId);

    record NotificationData(String id, Long memberId, String type, String title, String body,
            LocalDateTime createdAt, boolean read, String targetKind, String applicationId,
            String chatId) {}
    record SettingsData(Long memberId, boolean matchRequest, boolean matchSchedule,
            boolean chat, boolean review, boolean marketing) {}
}
