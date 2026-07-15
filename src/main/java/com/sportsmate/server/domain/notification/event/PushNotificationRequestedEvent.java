package com.sportsmate.server.domain.notification.event;

import com.sportsmate.server.common.domain.Event;
import com.sportsmate.server.common.port.out.push.PushMessage;

public class PushNotificationRequestedEvent extends Event {

    private final PushMessage message;

    public PushNotificationRequestedEvent(PushMessage message) {
        this.message = message;
    }

    public PushMessage getMessage() {
        return message;
    }
}
