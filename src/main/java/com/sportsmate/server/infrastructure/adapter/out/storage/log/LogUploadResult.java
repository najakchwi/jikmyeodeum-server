package com.sportsmate.server.infrastructure.adapter.out.storage.log;

import java.util.List;

public record LogUploadResult(
        List<String> uploadedFiles,
        List<String> failedFiles
) {
}
