package com.sportsmate.server.common.port.out.storage;

public record StoredObject(
        String objectKey,
        String url,
        String contentType,
        long contentLength
) {
}
