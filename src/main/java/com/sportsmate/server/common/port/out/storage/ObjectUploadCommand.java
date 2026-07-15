package com.sportsmate.server.common.port.out.storage;

import java.io.InputStream;

/**
 * 데이터 업로드할 때 정보를 담는 클래스
 */
public record ObjectUploadCommand(
        String objectKey,
        String contentType,
        long contentLength,
        InputStream inputStream
) {
}
