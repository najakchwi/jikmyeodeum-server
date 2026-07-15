package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class HealthAlertScheduler {

    static final String HEALTH_DEDUPE_KEY = "health:down";
    static final String MATCHING_JOB = "matching";
    static final String MATCHING_DEADMAN_DEDUPE_KEY = "job:matching:missing";

    private final OpsAlertPort opsAlertPort;
    private final JobHeartbeat jobHeartbeat;
    private final Clock clock;
    private final Supplier<Status> healthStatusSupplier;
    private final int downThreshold;
    private final Duration matchingGrace;
    private int consecutiveDownCount;

    @Autowired
    public HealthAlertScheduler(
            HealthEndpoint healthEndpoint,
            OpsAlertPort opsAlertPort,
            JobHeartbeat jobHeartbeat,
            @Value("${app.alert.health.down-threshold:3}") int downThreshold,
            @Value("${app.alert.matching.deadman-grace:PT30M}") Duration matchingGrace) {
        this(healthEndpoint, opsAlertPort, jobHeartbeat, Clock.systemDefaultZone(), downThreshold, matchingGrace);
    }

    HealthAlertScheduler(HealthEndpoint healthEndpoint, OpsAlertPort opsAlertPort, JobHeartbeat jobHeartbeat,
            Clock clock, int downThreshold, Duration matchingGrace) {
        this(() -> healthEndpoint.health().getStatus(), opsAlertPort, jobHeartbeat,
                clock, downThreshold, matchingGrace);
    }

    HealthAlertScheduler(Supplier<Status> healthStatusSupplier, OpsAlertPort opsAlertPort, JobHeartbeat jobHeartbeat,
            Clock clock, int downThreshold, Duration matchingGrace) {
        this.opsAlertPort = opsAlertPort;
        this.jobHeartbeat = jobHeartbeat;
        this.clock = clock;
        this.healthStatusSupplier = healthStatusSupplier;
        this.downThreshold = downThreshold;
        this.matchingGrace = matchingGrace;
    }

    @Scheduled(fixedRateString = "${app.alert.health.check-rate-ms:60000}")
    public void checkHealth() {
        Status status = healthStatusSupplier.get();
        if (Status.UP.equals(status)) {
            consecutiveDownCount = 0;
            opsAlertPort.resolve(HEALTH_DEDUPE_KEY);
            return;
        }
        consecutiveDownCount++;
        if (consecutiveDownCount >= downThreshold) {
            opsAlertPort.notify(AlertSeverity.CRITICAL, new AlertMessage(
                    "Health DOWN",
                    "Actuator health status is " + status.getCode(),
                    Map.of(
                            "status", status.getCode(),
                            "consecutiveDown", String.valueOf(consecutiveDownCount)),
                    HEALTH_DEDUPE_KEY));
        }
    }

    @Scheduled(cron = "0 */5 9-23 * * *", zone = "Asia/Seoul")
    public void checkMatchingDeadMan() {
        LocalTime threshold = LocalTime.of(9, 0).plus(matchingGrace);
        if (LocalTime.now(clock).isBefore(threshold)) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        if (jobHeartbeat.hasSuccessOn(MATCHING_JOB, today)) {
            opsAlertPort.resolve(MATCHING_DEADMAN_DEDUPE_KEY);
            return;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("job", MATCHING_JOB);
        fields.put("date", today.toString());
        fields.put("threshold", threshold.toString());
        jobHeartbeat.lastSuccess(MATCHING_JOB).ifPresent(last -> fields.put("lastSuccess", last.toString()));
        opsAlertPort.notify(AlertSeverity.CRITICAL, new AlertMessage(
                "매칭 배치 미실행",
                "09:00 정기 매칭 배치 성공 기록이 유예시간 이후에도 없습니다.",
                fields,
                MATCHING_DEADMAN_DEDUPE_KEY));
    }
}
