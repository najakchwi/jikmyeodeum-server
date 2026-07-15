package com.sportsmate.server.infrastructure.adapter.out.auth;

import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.port.out.SignupVerificationPort;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.store.type", havingValue = "memory", matchIfMissing = true)
public class InMemorySignupVerificationAdapter implements SignupVerificationPort {
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final int SEND_BURST_MAX_ATTEMPTS = 3;
    private static final long SEND_BURST_WINDOW_SECONDS = 600;
    private static final int SEND_DAILY_MAX_ATTEMPTS = 5;
    private static final long SEND_DAILY_WINDOW_SECONDS = 86_400;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final ConcurrentHashMap<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SignupTokenEntry> tokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PasswordResetTokenEntry> passwordResetTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SignupTokenEntry> socialPendingTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> sendAttempts = new ConcurrentHashMap<>();

    @Override
    public void saveCode(String purpose, String phone, String code, long expiresInSeconds) {
        codes.put(key(purpose, phone), new CodeEntry(code, Instant.now().plusSeconds(expiresInSeconds)));
    }

    @Override
    public CodeStatus verifyCode(String purpose, String phone, String code) {
        String key = key(purpose, phone);
        CodeEntry entry = codes.get(key);
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            codes.remove(key);
            return CodeStatus.EXPIRED;
        }
        if (!entry.code.equals(code)) {
            if (entry.failCount.incrementAndGet() >= MAX_VERIFY_ATTEMPTS) {
                codes.remove(key);
                return CodeStatus.LOCKED;
            }
            return CodeStatus.INVALID;
        }
        codes.remove(key);
        return CodeStatus.VALID;
    }

    @Override
    public String issuePhoneSignupToken(String phone, long expiresInSeconds) {
        return issue(new SignupIdentity(phone, LoginType.PHONE, null), expiresInSeconds);
    }

    @Override
    public String issueSocialSignupToken(String phone, LoginType loginType, String providerId, long expiresInSeconds) {
        return issue(new SignupIdentity(phone, loginType, providerId), expiresInSeconds);
    }

    @Override
    public String issuePasswordResetToken(String phone, long expiresInSeconds) {
        String token = UUID.randomUUID().toString();
        passwordResetTokens.put(token, new PasswordResetTokenEntry(phone, Instant.now().plusSeconds(expiresInSeconds)));
        return token;
    }

    @Override
    public String issueSocialPendingToken(LoginType loginType, String providerId, long expiresInSeconds) {
        String token = UUID.randomUUID().toString();
        socialPendingTokens.put(token, new SignupTokenEntry(
                new SignupIdentity(null, loginType, providerId),
                Instant.now().plusSeconds(expiresInSeconds)));
        return token;
    }

    @Override
    public Optional<SignupIdentity> findSocialPendingToken(String token) {
        SignupTokenEntry entry = socialPendingTokens.get(token);
        if (entry == null || !entry.expiresAt().isAfter(Instant.now())) {
            socialPendingTokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.identity());
    }

    @Override
    public Optional<SignupIdentity> consumeSocialPendingToken(String token) {
        SignupTokenEntry entry = socialPendingTokens.remove(token);
        if (entry == null || !entry.expiresAt().isAfter(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.identity());
    }

    @Override
    public Optional<SignupIdentity> consumeSignupToken(String token) {
        SignupTokenEntry entry = tokens.remove(token);
        if (entry == null || !entry.expiresAt().isAfter(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.identity());
    }

    @Override
    public Optional<String> consumePasswordResetToken(String token) {
        PasswordResetTokenEntry entry = passwordResetTokens.remove(token);
        if (entry == null || !entry.expiresAt().isAfter(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.phone());
    }

    @Override
    public SendAttemptStatus checkSendAttempt(String purpose, String phone) {
        Deque<Instant> attempts = sendAttempts.computeIfAbsent(key(purpose, phone),
                key -> new ConcurrentLinkedDeque<>());
        pruneExpired(attempts);

        Instant now = Instant.now();
        Instant last = attempts.peekLast();
        if (last != null) {
            long cooldownRemaining = last.getEpochSecond() + RESEND_COOLDOWN_SECONDS - now.getEpochSecond();
            if (cooldownRemaining > 0) {
                return new SendAttemptStatus(false, 0, cooldownRemaining, LimitReason.COOLDOWN);
            }
        }

        long dailyRetryAfter = retryAfter(attempts, now, SEND_DAILY_WINDOW_SECONDS, SEND_DAILY_MAX_ATTEMPTS);
        if (dailyRetryAfter >= 0) {
            return new SendAttemptStatus(false, 0, dailyRetryAfter, LimitReason.DAILY);
        }

        long burstCount = attempts.stream()
                .filter(attempt -> attempt.isAfter(now.minusSeconds(SEND_BURST_WINDOW_SECONDS)))
                .count();
        int remainingAttempts = Math.max(0, SEND_BURST_MAX_ATTEMPTS - (int) burstCount);
        if (remainingAttempts <= 0) {
            long burstRetryAfter = retryAfter(attempts, now, SEND_BURST_WINDOW_SECONDS, SEND_BURST_MAX_ATTEMPTS);
            return new SendAttemptStatus(false, 0, Math.max(0, burstRetryAfter), LimitReason.BURST);
        }
        return new SendAttemptStatus(true, remainingAttempts, 0, LimitReason.NONE);
    }

    @Override
    public void recordSendAttempt(String purpose, String phone) {
        Deque<Instant> attempts = sendAttempts.computeIfAbsent(key(purpose, phone),
                key -> new ConcurrentLinkedDeque<>());
        attempts.addLast(Instant.now());
        pruneExpired(attempts);
    }

    private void pruneExpired(Deque<Instant> attempts) {
        Instant threshold = Instant.now().minusSeconds(SEND_DAILY_WINDOW_SECONDS);
        Instant oldest;
        while ((oldest = attempts.peekFirst()) != null && oldest.isBefore(threshold)) {
            attempts.pollFirst();
        }
    }

    private long retryAfter(Deque<Instant> attempts, Instant now, long windowSeconds, int maxAttempts) {
        List<Instant> inWindow = attempts.stream()
                .filter(attempt -> attempt.isAfter(now.minusSeconds(windowSeconds)))
                .toList();
        if (inWindow.size() < maxAttempts) {
            return -1;
        }
        return Math.max(0, inWindow.get(0).getEpochSecond() + windowSeconds - now.getEpochSecond());
    }

    public void evictExpired() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
        tokens.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        passwordResetTokens.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        socialPendingTokens.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        sendAttempts.entrySet().removeIf(entry -> {
            pruneExpired(entry.getValue());
            return entry.getValue().isEmpty();
        });
    }

    private String issue(SignupIdentity identity, long expiresInSeconds) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new SignupTokenEntry(identity, Instant.now().plusSeconds(expiresInSeconds)));
        return token;
    }

    private String key(String purpose, String phone) {
        return purpose + ":" + phone;
    }

    private static final class CodeEntry {
        private final String code;
        private final Instant expiresAt;
        private final AtomicInteger failCount = new AtomicInteger();

        private CodeEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
    private record SignupTokenEntry(SignupIdentity identity, Instant expiresAt) {}
    private record PasswordResetTokenEntry(String phone, Instant expiresAt) {}
}
