package com.sportsmate.server.infrastructure.adapter.in.web.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.exception.CommonErrorCode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("ApiResponse 단위 테스트")
class ApiResponseTest {

    @Test
    @DisplayName("success(data)는 200 OK와 함께 성공 응답을 생성한다")
    void success_withData_returnsOkResponse() {
        var response = ApiResponse.success("data");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().data()).isEqualTo("data");
        assertThat(response.getBody().errorCode()).isNull();
        assertThat(response.getBody().message()).isNull();
        assertThat(response.getBody().details()).isNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("success()는 data가 null인 성공 응답을 생성한다")
    void success_withoutData_returnsOkResponseWithNullData() {
        var response = ApiResponse.success();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    @DisplayName("created(data)는 201 CREATED와 함께 성공 응답을 생성한다")
    void created_withData_returnsCreatedResponse() {
        var response = ApiResponse.created("data");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().data()).isEqualTo("data");
    }

    @Test
    @DisplayName("error(errorCode)는 errorCode의 상태/코드/메시지를 사용한 실패 응답을 생성한다")
    void error_withErrorCode_returnsErrorResponse() {
        var response = ApiResponse.error(CommonErrorCode.INVALID_INPUT);

        assertThat(response.getStatusCode()).isEqualTo(CommonErrorCode.INVALID_INPUT.getHttpStatus());
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().errorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT.getCode());
        assertThat(response.getBody().message()).isEqualTo(CommonErrorCode.INVALID_INPUT.getMessage());
        assertThat(response.getBody().details()).isNull();
    }

    @Test
    @DisplayName("error(errorCode, message)는 전달된 message로 실패 응답을 생성한다")
    void error_withCustomMessage_overridesMessage() {
        var response = ApiResponse.error(CommonErrorCode.INVALID_INPUT, "field 'email' is required");

        assertThat(response.getBody().errorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT.getCode());
        assertThat(response.getBody().message()).isEqualTo("field 'email' is required");
    }

    @Test
    @DisplayName("error(errorCode, message, details)는 상세 정보를 포함한 실패 응답을 생성한다")
    void error_withDetails_returnsErrorResponseWithDetails() {
        var details = Map.<String, Object>of("retryAfterSeconds", 120L);

        var response = ApiResponse.error(CommonErrorCode.INVALID_INPUT, "try later", details);

        assertThat(response.getBody().details()).isEqualTo(details);
    }
}
