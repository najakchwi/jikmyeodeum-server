package com.sportsmate.server.domain.notification.port.out;

public interface ChatMuteQueryPort {
    boolean isMuted(Long memberId, String chatId);
}
