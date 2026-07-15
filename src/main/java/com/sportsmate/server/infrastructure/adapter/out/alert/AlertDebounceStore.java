package com.sportsmate.server.infrastructure.adapter.out.alert;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AlertDebounceStore {

    private final Clock clock;
    private final Map<String, AlertState> states = new ConcurrentHashMap<>();

    public AlertDebounceStore() {
        this(Clock.systemDefaultZone());
    }

    AlertDebounceStore(Clock clock) {
        this.clock = clock;
    }

    public boolean shouldSend(String dedupeKey, Duration ttl) {
        if (dedupeKey == null || dedupeKey.isBlank()) {
            return true;
        }
        Instant now = Instant.now(clock);
        AlertState state = states.compute(dedupeKey, (key, current) -> {
            if (current == null || current.lastSent().plus(ttl).isBefore(now)) {
                return new AlertState(now);
            }
            return current;
        });
        return state.lastSent().equals(now);
    }

    public boolean markResolved(String dedupeKey) {
        if (dedupeKey == null || dedupeKey.isBlank()) {
            return false;
        }
        return states.remove(dedupeKey) != null;
    }

    private record AlertState(Instant lastSent) {
    }
}
