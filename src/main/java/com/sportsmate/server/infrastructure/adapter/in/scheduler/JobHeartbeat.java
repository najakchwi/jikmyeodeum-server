package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class JobHeartbeat {

    private final Clock clock;
    private final ConcurrentMap<String, LocalDateTime> successes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> failures = new ConcurrentHashMap<>();

    public JobHeartbeat() {
        this(Clock.systemDefaultZone());
    }

    JobHeartbeat(Clock clock) {
        this.clock = clock;
    }

    public void markSuccess(String jobName) {
        successes.put(jobName, LocalDateTime.now(clock));
        failures.remove(jobName);
    }

    public int markFailure(String jobName) {
        return failures.merge(jobName, 1, Integer::sum);
    }

    public void resetFailures(String jobName) {
        failures.remove(jobName);
    }

    public Optional<LocalDateTime> lastSuccess(String jobName) {
        return Optional.ofNullable(successes.get(jobName));
    }

    public boolean hasSuccessOn(String jobName, LocalDate date) {
        return lastSuccess(jobName).map(time -> time.toLocalDate().equals(date)).orElse(false);
    }
}
