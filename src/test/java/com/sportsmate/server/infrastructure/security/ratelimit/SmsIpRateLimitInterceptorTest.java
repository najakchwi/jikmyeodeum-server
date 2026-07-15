package com.sportsmate.server.infrastructure.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("SmsIpRateLimitInterceptor 단위 테스트")
class SmsIpRateLimitInterceptorTest {

    @Test
    @DisplayName("같은 IP에서 10분 내 10회를 초과하면 차단한다")
    void preHandle_afterMaxAttempts_throwsRateLimitExceeded() {
        var interceptor = new SmsIpRateLimitInterceptor();
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        var response = new MockHttpServletResponse();

        for (int i = 0; i < 10; i++) {
            interceptor.preHandle(request, response, new Object());
        }

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.IP_RATE_LIMIT_EXCEEDED);
    }
}
