package com.sportsmate.server.infrastructure.adapter.in.web.common;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final OpsAlertPort opsAlertPort;

    public GlobalExceptionHandler(OpsAlertPort opsAlertPort) {
        this.opsAlertPort = opsAlertPort;
    }

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getErrorCode(), e.getMessage(), e.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return ApiResponse.error(CommonErrorCode.INVALID_INPUT, message);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        IllegalArgumentException.class
    })
    public ApiResponse<Void> handleBadRequest(Exception e) {
        log.error("[BadRequest] {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ApiResponse.error(CommonErrorCode.INVALID_INPUT, "Invalid request");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ApiResponse.error(CommonErrorCode.INVALID_INPUT, "Uploaded file exceeds the maximum allowed size");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("[UnexpectedError] {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        notifyUnexpectedError(e);
        return ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private void notifyUnexpectedError(Exception exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("exception", exception.getClass().getName());
        if (MDC.get("traceId") != null) {
            fields.put("traceId", MDC.get("traceId"));
        }
        opsAlertPort.notify(AlertSeverity.CRITICAL, new AlertMessage(
                "미처리 500",
                exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                fields,
                "http:500:" + exception.getClass().getName() + ":" + Integer.toHexString(String.valueOf(exception.getMessage()).hashCode())));
    }
}
