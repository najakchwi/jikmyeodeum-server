package com.sportsmate.server.infrastructure.adapter.out.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.port.out.SignupVerificationPort;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemorySignupVerificationAdapter 단위 테스트")
class InMemorySignupVerificationAdapterTest {

    @Test
    @DisplayName("만료되지 않은 회원가입 토큰은 소비할 수 있다")
    void consumeSignupToken_beforeExpiry_returnsIdentity() {
        var adapter = new InMemorySignupVerificationAdapter();

        String token = adapter.issuePhoneSignupToken("01012345678", 60);

        assertThat(adapter.consumeSignupToken(token))
                .hasValueSatisfying(identity -> {
                    assertThat(identity.phone()).isEqualTo("01012345678");
                    assertThat(identity.loginType()).isEqualTo(LoginType.PHONE);
                });
    }

    @Test
    @DisplayName("만료된 회원가입 토큰은 소비할 수 없다")
    void consumeSignupToken_afterExpiry_returnsEmpty() throws InterruptedException {
        var adapter = new InMemorySignupVerificationAdapter();

        String token = adapter.issuePhoneSignupToken("01012345678", 0);
        Thread.sleep(5);

        assertThat(adapter.consumeSignupToken(token)).isEmpty();
    }

    @Test
    @DisplayName("비밀번호 재설정 토큰은 한 번만 소비할 수 있다")
    void consumePasswordResetToken_once_returnsPhoneThenEmpty() {
        var adapter = new InMemorySignupVerificationAdapter();

        String token = adapter.issuePasswordResetToken("01012345678", 60);

        assertThat(adapter.consumePasswordResetToken(token)).hasValue("01012345678");
        assertThat(adapter.consumePasswordResetToken(token)).isEmpty();
    }

    @Test
    @DisplayName("SMS 발송 상태는 남은 횟수와 재시도 대기 시간을 반환한다")
    void checkSendAttempt_afterMaxAttempts_returnsRetryAfterSeconds() {
        var adapter = new InMemorySignupVerificationAdapter();

        setAttempts(adapter, "01012345678",
                Instant.now().minusSeconds(180),
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(61));

        var status = adapter.checkSendAttempt("01012345678");

        assertThat(status.allowed()).isFalse();
        assertThat(status.remainingAttempts()).isZero();
        assertThat(status.retryAfterSeconds()).isPositive();
        assertThat(status.reason()).isEqualTo(SignupVerificationPort.LimitReason.BURST);
    }

    @Test
    @DisplayName("마지막 발송 후 60초가 지나지 않으면 쿨다운 상태를 반환한다")
    void checkSendAttempt_withRecentAttempt_returnsCooldown() {
        var adapter = new InMemorySignupVerificationAdapter();
        adapter.recordSendAttempt("01012345678");

        var status = adapter.checkSendAttempt("01012345678");

        assertThat(status.allowed()).isFalse();
        assertThat(status.reason()).isEqualTo(SignupVerificationPort.LimitReason.COOLDOWN);
        assertThat(status.retryAfterSeconds()).isPositive();
    }

    @Test
    @DisplayName("24시간 내 5회 발송하면 일일 상한 상태를 반환한다")
    void checkSendAttempt_afterDailyMaxAttempts_returnsDaily() {
        var adapter = new InMemorySignupVerificationAdapter();

        setAttempts(adapter, "01012345678",
                Instant.now().minusSeconds(25_000),
                Instant.now().minusSeconds(20_000),
                Instant.now().minusSeconds(15_000),
                Instant.now().minusSeconds(10_000),
                Instant.now().minusSeconds(61));

        var status = adapter.checkSendAttempt("01012345678");

        assertThat(status.allowed()).isFalse();
        assertThat(status.reason()).isEqualTo(SignupVerificationPort.LimitReason.DAILY);
        assertThat(status.retryAfterSeconds()).isPositive();
    }

    @Test
    @DisplayName("인증번호를 5회 틀리면 잠금 처리되고 이후 정답도 통과하지 않는다")
    void verifyCode_afterFiveFailures_locksAndRemovesCode() {
        var adapter = new InMemorySignupVerificationAdapter();
        adapter.saveCode("01012345678", "123456", 180);

        for (int i = 0; i < 4; i++) {
            assertThat(adapter.verifyCode("01012345678", "000000"))
                    .isEqualTo(SignupVerificationPort.CodeStatus.INVALID);
        }

        assertThat(adapter.verifyCode("01012345678", "000000"))
                .isEqualTo(SignupVerificationPort.CodeStatus.LOCKED);
        assertThat(adapter.verifyCode("01012345678", "123456"))
                .isEqualTo(SignupVerificationPort.CodeStatus.EXPIRED);
    }

    @Test
    @DisplayName("같은 전화번호라도 purpose가 다르면 발송 제한 카운터가 분리된다")
    void checkSendAttempt_differentPurpose_doesNotShareAttempts() {
        var adapter = new InMemorySignupVerificationAdapter();

        adapter.recordSendAttempt(SignupVerificationPort.PURPOSE_SIGNUP, "01012345678");

        var status = adapter.checkSendAttempt(SignupVerificationPort.PURPOSE_PASSWORD_RESET, "01012345678");

        assertThat(status.allowed()).isTrue();
        assertThat(status.remainingAttempts()).isEqualTo(3);
    }

    @SuppressWarnings("unchecked")
    private void setAttempts(InMemorySignupVerificationAdapter adapter, String phone, Instant... attempts) {
        try {
            Field field = InMemorySignupVerificationAdapter.class.getDeclaredField("sendAttempts");
            field.setAccessible(true);
            var sendAttempts = (ConcurrentHashMap<String, Deque<Instant>>) field.get(adapter);
            Deque<Instant> deque = new ConcurrentLinkedDeque<>();
            for (Instant attempt : attempts) {
                deque.addLast(attempt);
            }
            sendAttempts.put(SignupVerificationPort.PURPOSE_SIGNUP + ":" + phone, deque);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
