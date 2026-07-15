package com.sportsmate.server.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BusinessException 단위 테스트")
class BusinessExceptionTest {

    @Test
    @DisplayName("errorCode만으로 생성하면 errorCode의 message를 그대로 사용한다")
    void create_withErrorCodeOnly_usesErrorCodeMessage() {
        var exception = new BusinessException(CommonErrorCode.INVALID_INPUT);

        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT);
        assertThat(exception.getMessage()).isEqualTo(CommonErrorCode.INVALID_INPUT.getMessage());
        assertThat(exception.getDetails()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("detail이 있으면 message에 detail을 덧붙인다")
    void create_withDetail_appendsDetailToMessage() {
        var exception = new BusinessException(CommonErrorCode.INVALID_INPUT, "field 'email' is required");

        assertThat(exception.getMessage())
                .isEqualTo(CommonErrorCode.INVALID_INPUT.getMessage() + " - field 'email' is required");
    }

    @Test
    @DisplayName("details가 있으면 예외에 상세 정보를 보존한다")
    void create_withDetails_preservesDetails() {
        var details = Map.<String, Object>of("retryAfterSeconds", 120L);

        var exception = new BusinessException(CommonErrorCode.INVALID_INPUT, details);

        assertThat(exception.getDetails()).isEqualTo(details);
    }

    @Test
    @DisplayName("cause가 있으면 원인 예외를 보존한다")
    void create_withCause_preservesCause() {
        var cause = new IllegalStateException("root cause");

        var exception = new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "unexpected", cause);

        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getMessage())
                .isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage() + " - unexpected");
    }
}
