package com.sportsmate.server.domain.notification.port.in;

public interface PushTokenUseCase {
    void register(Long memberId, String token);
    void delete(Long memberId);
}
