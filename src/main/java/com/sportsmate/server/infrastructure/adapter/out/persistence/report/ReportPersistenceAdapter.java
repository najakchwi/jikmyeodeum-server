package com.sportsmate.server.infrastructure.adapter.out.persistence.report;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.report.port.out.ReportOutPort;

@PersistenceAdapter
public class ReportPersistenceAdapter implements ReportOutPort {
    private final ReportJpaRepository repository;

    public ReportPersistenceAdapter(ReportJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByReporterIdAndTargetUserIdAndChatId(Long reporterId, Long targetUserId,
            String chatId) {
        return repository.existsByReporterIdAndTargetMemberIdAndMatchId(
                reporterId, targetUserId, chatId == null ? null : Long.parseLong(chatId));
    }

    @Override public String save(Long reporterId, Long targetUserId, String applicationId,
            String chatId, String reason, String detail) {
        ReportEntity entity = ReportEntity.builder()
                .reporterId(reporterId)
                .targetMemberId(targetUserId)
                .matchId(chatId == null ? null : Long.parseLong(chatId))
                .reason(reason)
                .detail(detail)
                .status("received")
                .createdAt(java.time.LocalDateTime.now())
                .build();
        return String.valueOf(repository.save(entity).getId());
    }
}
