package com.sportsmate.server.domain.report.port.out;

public interface ReportOutPort {
    boolean existsByReporterIdAndTargetUserIdAndChatId(Long reporterId, Long targetUserId, String chatId);

    String save(Long reporterId, Long targetUserId, String applicationId, String chatId,
            String reason, String detail);
}
