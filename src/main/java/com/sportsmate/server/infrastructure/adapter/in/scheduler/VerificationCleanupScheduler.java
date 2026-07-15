package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import com.sportsmate.server.infrastructure.adapter.out.auth.InMemorySignupVerificationAdapter;
import com.sportsmate.server.infrastructure.security.ratelimit.SmsIpRateLimitInterceptor;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.store.type", havingValue = "memory", matchIfMissing = true)
public class VerificationCleanupScheduler {

    private final InMemorySignupVerificationAdapter verificationAdapter;
    private final SmsIpRateLimitInterceptor ipRateLimitInterceptor;
    private final OpsAlertPort opsAlertPort;
    private final JobHeartbeat jobHeartbeat;

    public VerificationCleanupScheduler(
            InMemorySignupVerificationAdapter verificationAdapter,
            SmsIpRateLimitInterceptor ipRateLimitInterceptor,
            OpsAlertPort opsAlertPort,
            JobHeartbeat jobHeartbeat) {
        this.verificationAdapter = verificationAdapter;
        this.ipRateLimitInterceptor = ipRateLimitInterceptor;
        this.opsAlertPort = opsAlertPort;
        this.jobHeartbeat = jobHeartbeat;
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        try {
            verificationAdapter.evictExpired();
            ipRateLimitInterceptor.evictExpired();
            jobHeartbeat.markSuccess("verification-cleanup");
        } catch (RuntimeException exception) {
            int failures = jobHeartbeat.markFailure("verification-cleanup");
            if (failures >= 3) {
                opsAlertPort.notify(AlertSeverity.WARNING, new AlertMessage(
                        "OTP 정리 반복 실패",
                        exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                        Map.of("job", "verification-cleanup", "consecutiveFailures", String.valueOf(failures)),
                        "job:verification-cleanup:failure"));
            }
            throw exception;
        }
    }
}
