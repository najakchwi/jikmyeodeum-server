package com.sportsmate.server.infrastructure.adapter.out.event;

import com.sportsmate.server.common.domain.Event;
import com.sportsmate.server.common.port.out.event.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventPublisherAdapter implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Event event) {
        applicationEventPublisher.publishEvent(event);
    }
}
