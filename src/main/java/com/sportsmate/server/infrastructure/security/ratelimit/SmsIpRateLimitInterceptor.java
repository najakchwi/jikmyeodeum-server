package com.sportsmate.server.infrastructure.security.ratelimit;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SmsIpRateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 600;

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = request.getRemoteAddr();
        Deque<Instant> attempts = attemptsByIp.computeIfAbsent(ip, key -> new ConcurrentLinkedDeque<>());
        synchronized (attempts) {
            pruneExpired(attempts);
            if (attempts.size() >= MAX_ATTEMPTS) {
                throw new BusinessException(CommonErrorCode.IP_RATE_LIMIT_EXCEEDED);
            }
            attempts.addLast(Instant.now());
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
}
