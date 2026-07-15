package com.sportsmate.server.infrastructure.monitoring;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import com.sportsmate.server.common.port.out.monitoring.SafetySignalPort;
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
public class SafetySignalMonitor implements SafetySignalPort {

    private final OpsAlertPort opsAlertPort;
    private final Clock clock;
    private final Duration window;
    private final int criticalThreshold;
    private final Deque<Instant> reports = new ArrayDeque<>();
    private final Deque<Instant> withdrawals = new ArrayDeque<>();

    @Autowired
    public SafetySignalMonitor(
            OpsAlertPort opsAlertPort,
            @Value("${app.alert.safety.window:PT1H}") Duration window,
            @Value("${app.alert.safety.critical-threshold:10}") int criticalThreshold) {
        this(opsAlertPort, Clock.systemDefaultZone(), window, criticalThreshold);
    }

    SafetySignalMonitor(OpsAlertPort opsAlertPort, Clock clock, Duration window, int criticalThreshold) {
        this.opsAlertPort = opsAlertPort;
        this.clock = clock;
        this.window = window;
        this.criticalThreshold = criticalThreshold;
    }

    @Override
    public void recordReport() {
        record(reports);
    }

    @Override
    public void recordWithdrawal() {
        record(withdrawals);
    }

    public void check() {
        Snapshot snapshot = snapshot();
        if (snapshot.reports() >= criticalThreshold || snapshot.withdrawals() >= criticalThreshold) {
            opsAlertPort.notify(AlertSeverity.CRITICAL, new AlertMessage(
                    "신고·탈퇴 폭증",
                    "Safety signal count exceeded the rolling-window threshold.",
                    Map.of(
                            "reports", String.valueOf(snapshot.reports()),
                            "withdrawals", String.valueOf(snapshot.withdrawals()),
                            "threshold", String.valueOf(criticalThreshold)),
                    "safety:spike"));
        } else {
            opsAlertPort.resolve("safety:spike");
        }
    }

    public Snapshot snapshot() {
        synchronized (this) {
            prune(reports);
            prune(withdrawals);
            return new Snapshot(reports.size(), withdrawals.size());
        }
    }

    private void record(Deque<Instant> events) {
        synchronized (this) {
            events.addLast(Instant.now(clock));
            prune(events);
        }
    }

    private void prune(Deque<Instant> events) {
        Instant threshold = Instant.now(clock).minus(window);
        while (!events.isEmpty() && events.peekFirst().isBefore(threshold)) {
            events.pollFirst();
        }
    }

    public record Snapshot(int reports, int withdrawals) {
    }
}
