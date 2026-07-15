package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.infrastructure.adapter.out.auth.InMemorySignupVerificationAdapter;
import com.sportsmate.server.infrastructure.security.ratelimit.SmsIpRateLimitInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.store.type", havingValue = "memory", matchIfMissing = true)
public class VerificationCleanupScheduler {

    private final InMemorySignupVerificationAdapter verificationAdapter;
    private final SmsIpRateLimitInterceptor ipRateLimitInterceptor;

    public VerificationCleanupScheduler(
            InMemorySignupVerificationAdapter verificationAdapter,
            SmsIpRateLimitInterceptor ipRateLimitInterceptor) {
        this.verificationAdapter = verificationAdapter;
        this.ipRateLimitInterceptor = ipRateLimitInterceptor;
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        verificationAdapter.evictExpired();
        ipRateLimitInterceptor.evictExpired();
    }
}
