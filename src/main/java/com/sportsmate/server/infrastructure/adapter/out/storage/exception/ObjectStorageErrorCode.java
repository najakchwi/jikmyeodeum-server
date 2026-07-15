package com.sportsmate.server.infrastructure.adapter.out.storage.exception;

import com.sportsmate.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ObjectStorageErrorCode implements ErrorCode {

    // Bad Request
    INVALID_OBJECT_KEY("O400_1", "Invalid object key", HttpStatus.BAD_REQUEST),

    // Not Found
    OBJECT_NOT_FOUND("O404_1", "Object not found", HttpStatus.NOT_FOUND),

    // Server Error
    UPLOAD_FAILED("O500_1", "Object upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
    DELETE_FAILED("O500_2", "Object delete failed", HttpStatus.INTERNAL_SERVER_ERROR),
    URL_GENERATION_FAILED("O500_3", "Object URL generation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    DOWNLOAD_FAILED("O500_4", "Object download failed", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
