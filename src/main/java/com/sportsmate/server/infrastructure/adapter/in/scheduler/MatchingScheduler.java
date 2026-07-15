package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import com.sportsmate.server.domain.application.port.in.ApplicationUseCase;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MatchingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchingScheduler.class);

    private final ApplicationUseCase applicationUseCase;
    private final OpsAlertPort opsAlertPort;
    private final JobHeartbeat jobHeartbeat;
    private final double matchRateWarningThreshold;

    public MatchingScheduler(
            ApplicationUseCase applicationUseCase,
            OpsAlertPort opsAlertPort,
            JobHeartbeat jobHeartbeat,
            @Value("${app.alert.matching.match-rate-warning-threshold:0.4}") double matchRateWarningThreshold) {
        this.applicationUseCase = applicationUseCase;
        this.opsAlertPort = opsAlertPort;
        this.jobHeartbeat = jobHeartbeat;
        this.matchRateWarningThreshold = matchRateWarningThreshold;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void matchWaitingApplications() {
        opsAlertPort.notify(AlertSeverity.INFO, AlertMessage.of(
                "매칭 배치 시작",
                "정기 매칭 배치를 시작했습니다.",
                "job:matching:start:" + LocalDateTime.now().toLocalDate()));
        try {
            var result = applicationUseCase.matchWaitingApplications();
            log.info(
                    "Waiting applications matched. gamesProcessed={}, gamesFailed={}, pairsMatched={}, totalApplicants={}, unmatchedPeople={}, personErrors={}, carryOver={}, durationMs={}",
                    result.gamesProcessed(),
                    result.gamesFailed(),
                    result.pairsMatched(),
                    result.totalApplicants(),
                    result.unmatchedPeople(),
                    result.personErrors(),
                    result.carryOver(),
                    result.durationMs());
            jobHeartbeat.markSuccess(HealthAlertScheduler.MATCHING_JOB);
            opsAlertPort.resolve(HealthAlertScheduler.MATCHING_DEADMAN_DEDUPE_KEY);
            opsAlertPort.notify(AlertSeverity.METRIC, matchingSummary(result));
            if (result.gamesFailed() > 0 || result.matchRate() < matchRateWarningThreshold) {
                log.warn("Waiting applications matching quality degraded. gamesFailed={}, matchRate={}",
                        result.gamesFailed(), result.matchRate());
                opsAlertPort.notify(AlertSeverity.WARNING, matchingWarning(result));
            }
        } catch (RuntimeException exception) {
            log.error("Waiting applications matching failed fatally.", exception);
            jobHeartbeat.markFailure(HealthAlertScheduler.MATCHING_JOB);
            opsAlertPort.notify(AlertSeverity.CRITICAL, new AlertMessage(
                    "매칭 배치 치명실패",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    Map.of("job", HealthAlertScheduler.MATCHING_JOB),
                    "job:matching:fatal"));
        }
    }

    private AlertMessage matchingSummary(ApplicationUseCase.MatchBatchResult result) {
        return new AlertMessage(
                "매칭 배치 완료",
                "경기 " + result.gamesProcessed() + "개 처리 · 매칭 " + result.pairsMatched()
                        + "쌍 · 매칭률 " + percent(result.matchRate()),
                matchingFields(result),
                null);
    }

    private AlertMessage matchingWarning(ApplicationUseCase.MatchBatchResult result) {
        return new AlertMessage(
                "매칭 품질 저하",
                "경기 실패가 있거나 매칭률이 임계값보다 낮습니다.",
                matchingFields(result),
                "job:matching:quality");
    }

    private Map<String, String> matchingFields(ApplicationUseCase.MatchBatchResult result) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("처리 경기", String.valueOf(result.gamesProcessed()));
        fields.put("경기 실패", String.valueOf(result.gamesFailed()));
        fields.put("총 신청", String.valueOf(result.totalApplicants()));
        fields.put("매칭 쌍", String.valueOf(result.pairsMatched()));
        fields.put("매칭 인원", String.valueOf(result.matchedPeople()));
        fields.put("미매칭", String.valueOf(result.unmatchedPeople()));
        fields.put("개인 오류", String.valueOf(result.personErrors()));
        fields.put("이월", String.valueOf(result.carryOver()));
        fields.put("매칭률", percent(result.matchRate()));
        fields.put("소요", result.durationMs() + "ms");
        return fields;
    }

    private String percent(double value) {
        return String.format("%.1f%%", value * 100);
    }
}
