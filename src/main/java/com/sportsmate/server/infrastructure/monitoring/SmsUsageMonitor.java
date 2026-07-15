package com.sportsmate.server.infrastructure.monitoring;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import com.sportsmate.server.common.port.out.monitoring.SmsUsagePort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsUsageMonitor implements SmsUsagePort {

    private final OpsAlertPort opsAlertPort;
    private final Clock clock;
    private final Duration window;
    private final int sentWarningThreshold;
    private final int blockedWarningThreshold;
    private final Deque<SmsEvent> events = new ArrayDeque<>();

    @Autowired
    public SmsUsageMonitor(
            OpsAlertPort opsAlertPort,
            @Value("${app.alert.sms.window:PT1H}") Duration window,
            @Value("${app.alert.sms.sent-warning-threshold:100}") int sentWarningThreshold,
            @Value("${app.alert.sms.blocked-warning-threshold:20}") int blockedWarningThreshold) {
        this(opsAlertPort, Clock.systemDefaultZone(), window, sentWarningThreshold, blockedWarningThreshold);
    }

    SmsUsageMonitor(OpsAlertPort opsAlertPort, Clock clock, Duration window,
            int sentWarningThreshold, int blockedWarningThreshold) {
        this.opsAlertPort = opsAlertPort;
        this.clock = clock;
        this.window = window;
        this.sentWarningThreshold = sentWarningThreshold;
        this.blockedWarningThreshold = blockedWarningThreshold;
    }

    @Override
    public void recordSent(boolean success) {
        record(new SmsEvent(Instant.now(clock), success, false));
    }

    @Override
    public void recordRateLimited() {
        record(new SmsEvent(Instant.now(clock), false, true));
    }

    public void check() {
        Snapshot snapshot = snapshot();
        if (snapshot.sent() >= sentWarningThreshold || snapshot.blocked() >= blockedWarningThreshold) {
            opsAlertPort.notify(AlertSeverity.WARNING, new AlertMessage(
                    "SMS 발송·차단 급증",
                    "SMS sent or rate-limit blocked count exceeded threshold.",
                    Map.of(
                            "sent", String.valueOf(snapshot.sent()),
                            "success", String.valueOf(snapshot.success()),
                            "failed", String.valueOf(snapshot.failed()),
                            "blocked", String.valueOf(snapshot.blocked())),
                    "sms:usage:spike"));
        } else {
            opsAlertPort.resolve("sms:usage:spike");
        }
    }

    public Snapshot snapshot() {
        synchronized (this) {
            prune();
            int sent = 0;
            int success = 0;
            int failed = 0;
            int blocked = 0;
            for (SmsEvent event : events) {
                if (event.blocked()) {
                    blocked++;
                } else {
                    sent++;
                    if (event.success()) {
                        success++;
                    } else {
                        failed++;
                    }
                }
            }
            return new Snapshot(sent, success, failed, blocked);
        }
    }

    private void record(SmsEvent event) {
        synchronized (this) {
            events.addLast(event);
            prune();
        }
    }

    private void prune() {
        Instant threshold = Instant.now(clock).minus(window);
        while (!events.isEmpty() && events.peekFirst().time().isBefore(threshold)) {
            events.pollFirst();
        }
    }

    private record SmsEvent(Instant time, boolean success, boolean blocked) {
    }

    public record Snapshot(int sent, int success, int failed, int blocked) {
        public String successRate() {
            return sent == 0 ? "100.0%" : String.format("%.1f%%", (double) success / sent * 100);
        }
    }
}
