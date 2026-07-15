package com.sportsmate.server.infrastructure.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SmsUsageMonitor 단위 테스트")
class SmsUsageMonitorTest {

    @Test
    @DisplayName("차단 카운트가 임계값 이상이면 warning 알림을 발송한다")
    void check_blockedOverThreshold_sendsWarning() {
        RecordingAlertPort alerts = new RecordingAlertPort();
        var monitor = new SmsUsageMonitor(
                alerts,
                Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneId.of("Asia/Seoul")),
                Duration.ofHours(1),
                100,
                2);

        monitor.recordRateLimited();
        monitor.recordRateLimited();
        monitor.check();

        assertThat(alerts.sent).hasSize(1);
        assertThat(alerts.sent.getFirst().severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    @DisplayName("발송 성공률 스냅샷을 계산한다")
    void snapshot_countsSuccessRate() {
        var monitor = new SmsUsageMonitor(
                new RecordingAlertPort(),
                Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneId.of("Asia/Seoul")),
                Duration.ofHours(1),
                100,
                20);

        monitor.recordSent(true);
        monitor.recordSent(false);

        assertThat(monitor.snapshot().successRate()).isEqualTo("50.0%");
    }

    private static class RecordingAlertPort implements OpsAlertPort {
        private final List<SentAlert> sent = new ArrayList<>();

        @Override
        public void notify(AlertSeverity severity, AlertMessage message) {
            sent.add(new SentAlert(severity, message));
        }

        @Override
        public void resolve(String dedupeKey) {
        }
    }

    private record SentAlert(AlertSeverity severity, AlertMessage message) {
    }
}
