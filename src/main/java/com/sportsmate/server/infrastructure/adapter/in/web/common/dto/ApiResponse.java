package com.sportsmate.server.infrastructure.adapter.in.web.common.dto;

import com.sportsmate.server.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@Schema(description = "공통 API 응답")
public class ApiResponse<T> extends ResponseEntity<ApiResponse.Body<T>> {

    private ApiResponse(Body<T> body, HttpStatusCode status) {
        super(body, status);
    }

    @Schema(description = "공통 API 응답 바디")
    public record Body<T>(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean isSuccess,

            @Schema(description = "성공 응답 데이터. 에러 응답에서는 null이다.", nullable = true)
            T data,

            @Schema(description = "에러 코드. 성공 응답에서는 null이다.", example = "G400", nullable = true)
            String errorCode,

            @Schema(description = "응답 메시지. 성공 응답에서는 null이다.", example = "Invalid input", nullable = true)
            String message,

            @Schema(description = "에러 상세 정보. 상세 정보가 없거나 성공 응답이면 null이다.", nullable = true)
            Map<String, Object> details,

            @Schema(description = "응답 생성 시각", example = "2024-01-01T00:00:00")
            LocalDateTime timestamp
    ) {
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, HttpStatus.OK);
    }

    public static <T> ApiResponse<T> success(T data, HttpStatusCode status) {
        return new ApiResponse<>(new Body<>(true, data, null, null, null, LocalDateTime.now()), status);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> created() {
        return success(null, HttpStatus.CREATED);
    }

    public static <T> ApiResponse<T> created(T data) {
        return success(data, HttpStatus.CREATED);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return error(errorCode, message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, Map<String, Object> details) {
        return error(errorCode.getCode(), message, errorCode.getHttpStatus(), details);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, HttpStatusCode status) {
        return error(errorCode, message, status, null);
    }

    public static <T> ApiResponse<T> error(
            String errorCode, String message, HttpStatusCode status, Map<String, Object> details) {
        return new ApiResponse<>(new Body<>(false, null, errorCode, message, details, LocalDateTime.now()), status);
    }
}
