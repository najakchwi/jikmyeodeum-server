package com.sportsmate.server.infrastructure.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MetricsAlertScheduler 단위 테스트")
class MetricsAlertSchedulerTest {

    @Test
    @DisplayName("swap 사용률이 임계값 이상이면 warning 알림을 발송한다")
    void checkMetrics_whenSwapUsageOverThreshold_sendsWarning() {
        RecordingAlertPort alerts = new RecordingAlertPort();
        var scheduler = scheduler(new SimpleMeterRegistry(), alerts, Map.of(
                "TotalSwapSpaceSize", Optional.of(100L * 1024 * 1024),
                "FreeSwapSpaceSize", Optional.of(85L * 1024 * 1024)));

        scheduler.checkMetrics();

        assertThat(alerts.sent)
                .extracting(alert -> alert.message().dedupeKey())
                .contains("metric:system:swap");
        SentAlert swapAlert = alerts.sent.stream()
                .filter(alert -> alert.message().dedupeKey().equals("metric:system:swap"))
                .findFirst()
                .orElseThrow();
        assertThat(swapAlert.severity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(swapAlert.message().fields())
                .containsEntry("usedMb", "15")
                .containsEntry("totalMb", "100")
                .containsEntry("value", "15.0%")
                .containsEntry("threshold", "10.0%");
    }

    @Test
    @DisplayName("swap 사용률이 임계값 미만이면 resolve를 호출한다")
    void checkMetrics_whenSwapUsageBelowThreshold_resolvesWarning() {
        RecordingAlertPort alerts = new RecordingAlertPort();
        var scheduler = scheduler(new SimpleMeterRegistry(), alerts, Map.of(
                "TotalSwapSpaceSize", Optional.of(100L * 1024 * 1024),
                "FreeSwapSpaceSize", Optional.of(95L * 1024 * 1024)));

        scheduler.checkMetrics();

        assertThat(alerts.resolved).contains("metric:system:swap");
    }

    @Test
    @DisplayName("disk 사용률이 임계값 이상이면 warning 알림을 발송한다")
    void checkMetrics_whenDiskUsageOverThreshold_sendsWarning() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RecordingAlertPort alerts = new RecordingAlertPort();
        AtomicLong total = new AtomicLong(100);
        AtomicLong free = new AtomicLong(19);
        Gauge.builder("disk.total", total, AtomicLong::doubleValue).register(registry);
        Gauge.builder("disk.free", free, AtomicLong::doubleValue).register(registry);
        var scheduler = scheduler(registry, alerts, Map.of());

        scheduler.checkMetrics();

        assertThat(alerts.sent)
                .extracting(alert -> alert.message().dedupeKey())
                .contains("metric:system:disk");
    }

    @Test
    @DisplayName("GC pause 평균이 임계값 이상이면 warning 알림을 발송한다")
    void checkMetrics_whenGcPauseOverThreshold_sendsWarning() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RecordingAlertPort alerts = new RecordingAlertPort();
        Timer timer = Timer.builder("jvm.gc.pause").register(registry);
        timer.record(1500, TimeUnit.MILLISECONDS);
        var scheduler = scheduler(registry, alerts, Map.of());

        scheduler.checkMetrics();

        assertThat(alerts.sent)
                .extracting(alert -> alert.message().dedupeKey())
                .contains("metric:jvm:gc");
    }

    private MetricsAlertScheduler scheduler(
            SimpleMeterRegistry registry,
            RecordingAlertPort alerts,
            Map<String, Optional<Long>> osAttributes) {
        return new MetricsAlertScheduler(
                registry,
                alerts,
                0.85,
                0.85,
                0.9,
                0.1,
                0.8,
                1000,
                3000,
                attribute -> osAttributes.getOrDefault(attribute, Optional.empty()));
    }

    private static class RecordingAlertPort implements OpsAlertPort {
        private final List<SentAlert> sent = new ArrayList<>();
        private final List<String> resolved = new ArrayList<>();

        @Override
        public void notify(AlertSeverity severity, AlertMessage message) {
            sent.add(new SentAlert(severity, message));
        }

        @Override
        public void resolve(String dedupeKey) {
            resolved.add(dedupeKey);
        }
    }

    private record SentAlert(AlertSeverity severity, AlertMessage message) {
    }
}
