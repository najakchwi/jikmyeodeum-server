package com.sportsmate.server.common.port.out.event;

import com.sportsmate.server.common.domain.Event;

public interface EventPublisher {

    void publish(Event event);
}
