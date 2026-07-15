package com.sportsmate.server.infrastructure.adapter.in.scheduler;

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
import org.springframework.boot.health.contributor.Status;

@DisplayName("HealthAlertScheduler 단위 테스트")
class HealthAlertSchedulerTest {

    @Test
    @DisplayName("Health DOWN 3회 연속부터 critical 알림을 발송한다")
    void checkHealth_afterThreeConsecutiveDown_sendsCritical() {
        RecordingAlertPort alerts = new RecordingAlertPort();
        var scheduler = new HealthAlertScheduler(
                () -> Status.DOWN, alerts, new JobHeartbeat(),
                Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneId.of("Asia/Seoul")),
                3, Duration.ofMinutes(30));

        scheduler.checkHealth();
        scheduler.checkHealth();
        scheduler.checkHealth();

        assertThat(alerts.sent).hasSize(1);
        assertThat(alerts.sent.getFirst().severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alerts.sent.getFirst().message().dedupeKey()).isEqualTo(HealthAlertScheduler.HEALTH_DEDUPE_KEY);
    }

    @Test
    @DisplayName("Health가 UP으로 복귀하면 resolve를 호출한다")
    void checkHealth_whenUp_resolvesDownAlert() {
        RecordingAlertPort alerts = new RecordingAlertPort();
        var scheduler = new HealthAlertScheduler(
                () -> Status.UP, alerts, new JobHeartbeat(),
                Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneId.of("Asia/Seoul")),
                3, Duration.ofMinutes(30));

        scheduler.checkHealth();

        assertThat(alerts.resolved).containsExactly(HealthAlertScheduler.HEALTH_DEDUPE_KEY);
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
