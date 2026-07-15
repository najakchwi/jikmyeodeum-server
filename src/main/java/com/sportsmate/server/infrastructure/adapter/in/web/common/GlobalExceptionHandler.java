package com.sportsmate.server.infrastructure.adapter.in.web.common;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
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
        return ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}
