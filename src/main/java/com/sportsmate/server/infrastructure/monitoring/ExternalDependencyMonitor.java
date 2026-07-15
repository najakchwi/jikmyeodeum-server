package com.sportsmate.server.infrastructure.monitoring;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExternalDependencyMonitor {

    private final OpsAlertPort opsAlertPort;
    private final Clock clock;
    private final Duration window;
    private final double failureRateThreshold;
    private final ConcurrentMap<String, List<CallSample>> samplesByName = new ConcurrentHashMap<>();

    @Autowired
    public ExternalDependencyMonitor(
            OpsAlertPort opsAlertPort,
            @Value("${app.alert.external.window:PT5M}") Duration window,
            @Value("${app.alert.external.failure-rate-threshold:0.1}") double failureRateThreshold) {
        this(opsAlertPort, Clock.systemDefaultZone(), window, failureRateThreshold);
    }

    ExternalDependencyMonitor(OpsAlertPort opsAlertPort, Clock clock, Duration window, double failureRateThreshold) {
        this.opsAlertPort = opsAlertPort;
        this.clock = clock;
        this.window = window;
        this.failureRateThreshold = failureRateThreshold;
    }

    public <T> T observe(String name, Supplier<T> supplier) {
        long startedAt = System.nanoTime();
        try {
            T result = supplier.get();
            record(name, true, elapsedMs(startedAt), false);
            return result;
        } catch (RuntimeException exception) {
            record(name, false, elapsedMs(startedAt), isTimeout(exception));
            throw exception;
        }
    }

    public void observe(String name, Runnable runnable) {
        observe(name, () -> {
            runnable.run();
            return null;
        });
    }

    public void record(String name, boolean success, long latencyMs, boolean timeout) {
        samplesByName.computeIfAbsent(name, ignored -> new ArrayList<>());
        List<CallSample> samples = samplesByName.get(name);
        synchronized (samples) {
            samples.add(new CallSample(Instant.now(clock), success, latencyMs, timeout));
            prune(samples);
        }
    }

    public void check() {
        samplesByName.forEach((name, samples) -> {
            Snapshot snapshot;
            synchronized (samples) {
                prune(samples);
                snapshot = Snapshot.from(samples);
            }
            if (snapshot.total() == 0) {
                return;
            }
            if (snapshot.failureRate() >= failureRateThreshold || snapshot.timeouts() >= 3) {
                opsAlertPort.notify(AlertSeverity.WARNING, new AlertMessage(
                        "외부 API 장애",
                        name + " dependency failure rate is above threshold.",
                        Map.of(
                                "adapter", name,
                                "total", String.valueOf(snapshot.total()),
                                "failures", String.valueOf(snapshot.failures()),
                                "timeouts", String.valueOf(snapshot.timeouts()),
                                "failureRate", String.format("%.1f%%", snapshot.failureRate() * 100),
                                "avgLatencyMs", String.valueOf(snapshot.avgLatencyMs())),
                        "external:" + name));
            } else {
                opsAlertPort.resolve("external:" + name);
            }
        });
    }

    private long elapsedMs(long startedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private boolean isTimeout(RuntimeException exception) {
        String name = exception.getClass().getSimpleName().toLowerCase();
        return name.contains("timeout") || String.valueOf(exception.getMessage()).toLowerCase().contains("timeout");
    }

    private void prune(List<CallSample> samples) {
        Instant threshold = Instant.now(clock).minus(window);
        Iterator<CallSample> iterator = samples.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().time().isBefore(threshold)) {
                iterator.remove();
            }
        }
    }

    private record CallSample(Instant time, boolean success, long latencyMs, boolean timeout) {
    }

    private record Snapshot(int total, int failures, int timeouts, long avgLatencyMs) {
        static Snapshot from(List<CallSample> samples) {
            int failures = 0;
            int timeouts = 0;
            long latency = 0;
            for (CallSample sample : samples) {
                if (!sample.success()) {
                    failures++;
                }
                if (sample.timeout()) {
                    timeouts++;
                }
                latency += sample.latencyMs();
            }
            return new Snapshot(samples.size(), failures, timeouts, samples.isEmpty() ? 0 : latency / samples.size());
        }

        double failureRate() {
            return total == 0 ? 0 : (double) failures / total;
        }
    }
}
