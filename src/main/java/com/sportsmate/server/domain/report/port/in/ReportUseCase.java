package com.sportsmate.server.domain.report.port.in;

public interface ReportUseCase {
    ReportResult report(Long reporterId, Long targetUserId, String applicationId,
            String chatId, String reason, String detail);
    record ReportResult(String id, String status) {}
}
