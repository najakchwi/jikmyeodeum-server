package com.sportsmate.server.common.port.out.push;

public interface PushOutPort {
    void send(PushMessage message);
}
