package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import com.sportsmate.server.domain.application.port.in.ApplicationUseCase;
import com.sportsmate.server.infrastructure.monitoring.MatchingBatchMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

@DisplayName("MatchingScheduler 단위 테스트")
class MatchingSchedulerTest {

    @Test
    @DisplayName("정상 종료 시 metrics 알림과 하트비트를 남긴다")
    void matchWaitingApplications_success_sendsMetricAndHeartbeat() {
        RecordingAlertPort alerts = new RecordingAlertPort();
        JobHeartbeat heartbeat = new JobHeartbeat();
        var scheduler = new MatchingScheduler(
                new StubApplicationUseCase(new ApplicationUseCase.MatchBatchResult(1, 0, 0, 1, 2, 0, 0, 0, 10)),
                alerts,
                heartbeat,
                metrics(),
                0.4);

        scheduler.matchWaitingApplications();

        assertThat(alerts.sent).anyMatch(alert -> alert.severity() == AlertSeverity.METRIC);
        assertThat(heartbeat.hasSuccessOn(HealthAlertScheduler.MATCHING_JOB, LocalDate.now())).isTrue();
    }

    @Test
    @DisplayName("경기 실패가 있으면 warning 알림을 발송한다")
    void matchWaitingApplications_withGameFailure_sendsWarning() {
        RecordingAlertPort alerts = new RecordingAlertPort();
        var scheduler = new MatchingScheduler(
                new StubApplicationUseCase(new ApplicationUseCase.MatchBatchResult(2, 1, 0, 1, 4, 2, 0, 2, 10)),
                alerts,
                new JobHeartbeat(),
                metrics(),
                0.4);

        scheduler.matchWaitingApplications();

        assertThat(alerts.sent).anyMatch(alert -> alert.severity() == AlertSeverity.WARNING);
    }

    @Test
    @DisplayName("배치 예외는 critical 알림으로 전환한다")
    void matchWaitingApplications_whenFatal_sendsCritical() {
        RecordingAlertPort alerts = new RecordingAlertPort();
        var scheduler = new MatchingScheduler(
                new ThrowingApplicationUseCase(),
                alerts,
                new JobHeartbeat(),
                metrics(),
                0.4);

        scheduler.matchWaitingApplications();

        assertThat(alerts.sent).anyMatch(alert -> alert.severity() == AlertSeverity.CRITICAL);
    }

    @Test
    @DisplayName("매칭 스케줄 기본 크론은 9시·15시·21시 하루 3회다")
    void matchWaitingApplications_hasTripleDailyDefaultCron() throws NoSuchMethodException {
        Scheduled scheduled = MatchingScheduler.class
                .getMethod("matchWaitingApplications")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${app.matching.schedule-cron:0 0 9,15,21 * * *}");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    private static class StubApplicationUseCase implements ApplicationUseCase {
        private final MatchBatchResult result;

        private StubApplicationUseCase(MatchBatchResult result) {
            this.result = result;
        }

        @Override public MatchBatchResult matchWaitingApplications() { return result; }
        @Override public ApplicationResult apply(Long memberId, String gameId) { return null; }
        @Override public List<ApplicationResult> applications(Long memberId, java.time.LocalDate date, List<String> statuses) { return List.of(); }
        @Override public List<java.time.LocalDate> calendar(Long memberId, int year, int month) { return List.of(); }
        @Override public ApplicationResult get(Long memberId, String applicationId) { return null; }
        @Override public void cancel(Long memberId, String applicationId) {}
        @Override public MatchStatusResult status(Long memberId, String applicationId) { return null; }
        @Override public ApplicationResult accept(Long memberId, String applicationId) { return null; }
        @Override public ApplicationResult reject(Long memberId, String applicationId) { return null; }
        @Override public void cancelAllActiveByMember(Long memberId) {}
    }

    private static class ThrowingApplicationUseCase extends StubApplicationUseCase {
        private ThrowingApplicationUseCase() {
            super(null);
        }

        @Override
        public MatchBatchResult matchWaitingApplications() {
            throw new IllegalStateException("boom");
        }
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

    private MatchingBatchMetrics metrics() {
        return new MatchingBatchMetrics(new SimpleMeterRegistry());
    }
}
