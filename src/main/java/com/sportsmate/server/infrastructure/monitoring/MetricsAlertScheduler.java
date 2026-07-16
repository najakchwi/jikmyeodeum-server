package com.sportsmate.server.infrastructure.monitoring;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.management.ObjectName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class MetricsAlertScheduler {

    private final MeterRegistry meterRegistry;
    private final OpsAlertPort opsAlertPort;
    private final double heapThreshold;
    private final double systemThreshold;
    private final double hikariThreshold;
    private final double swapThreshold;
    private final double diskThreshold;
    private final long gcPauseWarningMs;
    private final long latencyP99WarningMs;
    private final Function<String, Optional<Long>> osAttributeReader;
    private long previousServerErrors;
    private long previousHttpRequests;
    private long previousGcPauseCount;
    private double previousGcPauseTotalMs;

    @Autowired
    public MetricsAlertScheduler(
            MeterRegistry meterRegistry,
            OpsAlertPort opsAlertPort,
            @Value("${app.alert.metrics.heap-threshold:0.85}") double heapThreshold,
            @Value("${app.alert.metrics.system-threshold:0.85}") double systemThreshold,
            @Value("${app.alert.metrics.hikari-threshold:0.9}") double hikariThreshold,
            @Value("${app.alert.metrics.swap-threshold:0.1}") double swapThreshold,
            @Value("${app.alert.metrics.disk-threshold:0.8}") double diskThreshold,
            @Value("${app.alert.metrics.gc-pause-warning-ms:1000}") long gcPauseWarningMs,
            @Value("${app.alert.metrics.latency-p99-warning-ms:3000}") long latencyP99WarningMs) {
        this(meterRegistry, opsAlertPort, heapThreshold, systemThreshold, hikariThreshold,
                swapThreshold, diskThreshold, gcPauseWarningMs, latencyP99WarningMs,
                MetricsAlertScheduler::platformAttributeLong);
    }

    MetricsAlertScheduler(
            MeterRegistry meterRegistry,
            OpsAlertPort opsAlertPort,
            double heapThreshold,
            double systemThreshold,
            double hikariThreshold,
            double swapThreshold,
            double diskThreshold,
            long gcPauseWarningMs,
            long latencyP99WarningMs,
            Function<String, Optional<Long>> osAttributeReader) {
        this.meterRegistry = meterRegistry;
        this.opsAlertPort = opsAlertPort;
        this.heapThreshold = heapThreshold;
        this.systemThreshold = systemThreshold;
        this.hikariThreshold = hikariThreshold;
        this.swapThreshold = swapThreshold;
        this.diskThreshold = diskThreshold;
        this.gcPauseWarningMs = gcPauseWarningMs;
        this.latencyP99WarningMs = latencyP99WarningMs;
        this.osAttributeReader = osAttributeReader;
    }

    @Scheduled(fixedRateString = "${app.alert.metrics.check-rate-ms:60000}")
    public void checkMetrics() {
        checkHeap();
        checkCpu();
        checkSwap();
        checkDisk();
        checkGc();
        checkHikari();
        checkHttp5xxRate();
        checkLatency();
        checkThreads();
    }

    private void checkHeap() {
        Optional<Double> used = gauge("jvm.memory.used", "area", "heap");
        Optional<Double> max = gauge("jvm.memory.max", "area", "heap");
        if (used.isEmpty() || max.isEmpty() || max.get() <= 0) {
            return;
        }
        double ratio = used.get() / max.get();
        alertRatio(AlertSeverity.WARNING, "JVM Heap 사용률", "metric:jvm:heap", ratio, heapThreshold);
    }

    private void checkCpu() {
        Optional<Double> cpu = gauge("system.cpu.usage");
        if (cpu.isPresent()) {
            alertRatio(AlertSeverity.WARNING, "CPU 사용률", "metric:system:cpu", cpu.get(), systemThreshold);
        }
        long openFd = attributeLong("OpenFileDescriptorCount").orElse(0L);
        long maxFd = attributeLong("MaxFileDescriptorCount").orElse(0L);
        if (maxFd > 0) {
            alertRatio(AlertSeverity.WARNING, "FD 사용률", "metric:system:fd", (double) openFd / maxFd, 0.8);
        }
    }

    private void checkSwap() {
        Optional<Long> totalSwap = attributeLong("TotalSwapSpaceSize");
        Optional<Long> freeSwap = attributeLong("FreeSwapSpaceSize");
        if (totalSwap.isEmpty() || freeSwap.isEmpty() || totalSwap.get() <= 0) {
            return;
        }
        long used = Math.max(0, totalSwap.get() - freeSwap.get());
        alertRatio(AlertSeverity.WARNING, "Swap 사용률", "metric:system:swap",
                (double) used / totalSwap.get(), swapThreshold,
                Map.of(
                        "usedMb", String.valueOf(bytesToMb(used)),
                        "totalMb", String.valueOf(bytesToMb(totalSwap.get()))));
    }

    private void checkDisk() {
        Optional<Double> free = gauge("disk.free");
        Optional<Double> total = gauge("disk.total");
        if (free.isEmpty() || total.isEmpty() || total.get() <= 0) {
            return;
        }
        double usedRatio = 1.0 - (free.get() / total.get());
        alertRatio(AlertSeverity.WARNING, "Disk 사용률", "metric:system:disk", usedRatio, diskThreshold);
    }

    private void checkGc() {
        Timer timer = meterRegistry.find("jvm.gc.pause").timer();
        if (timer == null) {
            return;
        }
        long count = timer.count();
        double totalMs = timer.totalTime(TimeUnit.MILLISECONDS);
        long countDelta = count - previousGcPauseCount;
        double totalDelta = totalMs - previousGcPauseTotalMs;
        previousGcPauseCount = count;
        previousGcPauseTotalMs = totalMs;
        if (countDelta <= 0) {
            opsAlertPort.resolve("metric:jvm:gc");
            return;
        }
        double averagePauseMs = totalDelta / countDelta;
        if (averagePauseMs >= gcPauseWarningMs || countDelta >= 5) {
            opsAlertPort.notify(AlertSeverity.WARNING, new AlertMessage(
                    "GC 이상",
                    "GC pause count or average pause exceeded threshold.",
                    Map.of(
                            "countDelta", String.valueOf(countDelta),
                            "avgPauseMs", String.format("%.0f", averagePauseMs),
                            "thresholdMs", String.valueOf(gcPauseWarningMs)),
                    "metric:jvm:gc"));
        } else {
            opsAlertPort.resolve("metric:jvm:gc");
        }
    }

    private void checkHikari() {
        Optional<Double> active = gauge("hikaricp.connections.active");
        Optional<Double> max = gauge("hikaricp.connections.max");
        if (active.isPresent() && max.isPresent() && max.get() > 0) {
            alertRatio(AlertSeverity.WARNING, "Hikari Pool 사용률", "metric:hikari:pool",
                    active.get() / max.get(), hikariThreshold);
        }
    }

    private void checkHttp5xxRate() {
        long total = timerCount("http.server.requests", null);
        long errors = timerCount("http.server.requests", "5xx");
        long totalDelta = total - previousHttpRequests;
        long errorDelta = errors - previousServerErrors;
        previousHttpRequests = total;
        previousServerErrors = errors;
        if (totalDelta <= 0) {
            return;
        }
        double rate = (double) errorDelta / totalDelta;
        alertRatio(AlertSeverity.CRITICAL, "5xx 급증", "metric:http:5xx", rate, 0.05);
    }

    private void checkLatency() {
        Timer timer = meterRegistry.find("http.server.requests").timer();
        if (timer == null || timer.count() == 0) {
            return;
        }
        double p99Ms = p99OrMean(timer);
        if (p99Ms >= latencyP99WarningMs) {
            opsAlertPort.notify(AlertSeverity.WARNING, new AlertMessage(
                    "응답지연 증가",
                    "HTTP request p99 latency exceeded threshold.",
                    Map.of(
                            "p99Ms", String.format("%.0f", p99Ms),
                            "thresholdMs", String.valueOf(latencyP99WarningMs)),
                    "metric:http:latency"));
        } else {
            opsAlertPort.resolve("metric:http:latency");
        }
    }

    private double p99OrMean(Timer timer) {
        try {
            for (var percentile : timer.takeSnapshot().percentileValues()) {
                if (Math.abs(percentile.percentile() - 0.99) < 0.0001) {
                    return percentile.value(TimeUnit.MILLISECONDS);
                }
            }
        } catch (RuntimeException exception) {
            return timer.mean(TimeUnit.MILLISECONDS);
        }
        return timer.mean(TimeUnit.MILLISECONDS);
    }

    private long bytesToMb(long bytes) {
        return bytes / 1024 / 1024;
    }

    private void checkThreads() {
        Optional<Double> live = gauge("jvm.threads.live");
        if (live.isPresent() && live.get() >= 300) {
            opsAlertPort.notify(AlertSeverity.WARNING, new AlertMessage(
                    "Thread 이상",
                    "Live thread count exceeded threshold.",
                    Map.of("threads", String.format("%.0f", live.get())),
                    "metric:jvm:threads"));
        } else {
            opsAlertPort.resolve("metric:jvm:threads");
        }
    }

    private void alertRatio(AlertSeverity severity, String title, String dedupeKey, double ratio, double threshold) {
        alertRatio(severity, title, dedupeKey, ratio, threshold, Map.of());
    }

    private void alertRatio(
            AlertSeverity severity,
            String title,
            String dedupeKey,
            double ratio,
            double threshold,
            Map<String, String> extraFields) {
        if (ratio >= threshold) {
            Map<String, String> fields = new HashMap<>(extraFields);
            fields.put("value", String.format("%.1f%%", ratio * 100));
            fields.put("threshold", String.format("%.1f%%", threshold * 100));
            opsAlertPort.notify(severity, new AlertMessage(
                    title,
                    title + " is above threshold.",
                    Map.copyOf(fields),
                    dedupeKey));
        } else {
            opsAlertPort.resolve(dedupeKey);
        }
    }

    private Optional<Double> gauge(String name) {
        var gauge = meterRegistry.find(name).gauge();
        return gauge == null ? Optional.empty() : Optional.of(gauge.value());
    }

    private Optional<Double> gauge(String name, String tag, String value) {
        var gauge = meterRegistry.find(name).tag(tag, value).gauge();
        return gauge == null ? Optional.empty() : Optional.of(gauge.value());
    }

    private long timerCount(String name, String outcome) {
        var search = meterRegistry.find(name);
        if (outcome != null) {
            search.tag("outcome", outcome);
        }
        Timer timer = search.timer();
        return timer == null ? 0 : timer.count();
    }

    private Optional<Long> attributeLong(String attribute) {
        return osAttributeReader.apply(attribute);
    }

    private static Optional<Long> platformAttributeLong(String attribute) {
        try {
            Object value = ManagementFactory.getPlatformMBeanServer().getAttribute(
                    ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME), attribute);
            return value instanceof Number number ? Optional.of(number.longValue()) : Optional.empty();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
