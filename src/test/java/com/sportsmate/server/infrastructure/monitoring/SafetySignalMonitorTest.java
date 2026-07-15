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

@DisplayName("SafetySignalMonitor 단위 테스트")
class SafetySignalMonitorTest {

    @Test
    @DisplayName("신고 카운트가 임계값 이상이면 critical 알림을 발송한다")
    void check_reportsOverThreshold_sendsCritical() {
        RecordingAlertPort alerts = new RecordingAlertPort();
        var monitor = new SafetySignalMonitor(
                alerts,
                Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneId.of("Asia/Seoul")),
                Duration.ofHours(1),
                2);

        monitor.recordReport();
        monitor.recordReport();
        monitor.check();

        assertThat(alerts.sent).hasSize(1);
        assertThat(alerts.sent.getFirst().severity()).isEqualTo(AlertSeverity.CRITICAL);
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
