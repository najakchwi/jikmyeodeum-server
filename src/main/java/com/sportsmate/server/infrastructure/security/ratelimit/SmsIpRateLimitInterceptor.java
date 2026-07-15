package com.sportsmate.server.infrastructure.security.ratelimit;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.common.port.out.monitoring.SmsUsagePort;
import com.sportsmate.server.infrastructure.monitoring.ExternalDependencyMonitor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SmsIpRateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 600;

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();
    private final ExternalDependencyMonitor externalDependencyMonitor;
    private final SmsUsagePort smsUsagePort;

    public SmsIpRateLimitInterceptor() {
        this(null, null);
    }

    @Autowired
    public SmsIpRateLimitInterceptor(ExternalDependencyMonitor externalDependencyMonitor, SmsUsagePort smsUsagePort) {
        this.externalDependencyMonitor = externalDependencyMonitor;
        this.smsUsagePort = smsUsagePort;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = request.getRemoteAddr();
        Deque<Instant> attempts = attemptsByIp.computeIfAbsent(ip, key -> new ConcurrentLinkedDeque<>());
        synchronized (attempts) {
            pruneExpired(attempts);
            if (attempts.size() >= MAX_ATTEMPTS) {
                recordRateLimit(false);
                recordRateLimitedSms();
                throw new BusinessException(CommonErrorCode.IP_RATE_LIMIT_EXCEEDED);
            }
            attempts.addLast(Instant.now());
            recordRateLimit(true);
        }
        return true;
    }

    public void evictExpired() {
        attemptsByIp.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                pruneExpired(entry.getValue());
                return entry.getValue().isEmpty();
            }
        });
    }

    private void pruneExpired(Deque<Instant> attempts) {
        Instant threshold = Instant.now().minusSeconds(WINDOW_SECONDS);
        Instant oldest;
        while ((oldest = attempts.peekFirst()) != null && oldest.isBefore(threshold)) {
            attempts.pollFirst();
        }
    }

    private void recordRateLimit(boolean allowed) {
        if (externalDependencyMonitor != null) {
            externalDependencyMonitor.record("sms-rate-limit", allowed, 0, false);
        }
    }

    private void recordRateLimitedSms() {
        if (smsUsagePort != null) {
            try {
                smsUsagePort.recordRateLimited();
            } catch (RuntimeException exception) {
                // Monitoring must not affect rate-limit enforcement.
            }
        }
    }
}
