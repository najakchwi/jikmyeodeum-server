package com.sportsmate.server.infrastructure.adapter.in.event;

import com.sportsmate.server.common.port.out.push.PushOutPort;
import com.sportsmate.server.domain.notification.event.PushNotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PushNotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationEventListener.class);

    private final PushOutPort pushOutPort;

    public PushNotificationEventListener(PushOutPort pushOutPort) {
        this.pushOutPort = pushOutPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(PushNotificationRequestedEvent event) {
        try {
            pushOutPort.send(event.getMessage());
        } catch (RuntimeException exception) {
            log.warn("Failed to send push notification. token={}, type={}",
                    event.getMessage().to(), event.getMessage().data().get("type"), exception);
        }
    }
}
