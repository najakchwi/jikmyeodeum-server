package com.sportsmate.server.domain.chat.port.out;

public interface ChatRoomMuteOutPort {
    boolean isMuted(Long memberId, String chatId);
    void mute(Long memberId, String chatId);
    void unmute(Long memberId, String chatId);
}
