package com.sportsmate.server.infrastructure.adapter.out.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditEvent;
import com.sportsmate.server.common.port.out.audit.AuditResult;
import com.sportsmate.server.infrastructure.adapter.out.audit.entity.AuditLogEntity;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DisplayName("AuditLogAdapter 단위 테스트")
class AuditLogAdapterTest {

    private final AuditLogJpaRepository auditLogJpaRepository = Mockito.mock(AuditLogJpaRepository.class);
    private final AuditLogAdapter auditLogAdapter = new AuditLogAdapter(auditLogJpaRepository, new ObjectMapper());

    @Test
    @DisplayName("DB 저장 대상 카테고리는 파일 로그 후 audit_logs에 저장한다")
    void record_withPersistedCategory_savesAuditLog() {
        AuditEvent event = AuditEvent.of(
                AuditCategory.ADMIN_ACTION,
                "MANUAL_MATCHING_RUN",
                "ADMIN",
                "9",
                "MATCHING_BATCH",
                null,
                AuditResult.SUCCESS,
                Map.of("matchedCount", 3)
        );

        auditLogAdapter.record(event);

        verify(auditLogJpaRepository).save(any(AuditLogEntity.class));
    }

    @Test
    @DisplayName("DB 비저장 카테고리는 파일 로그만 남기고 audit_logs에 저장하지 않는다")
    void record_withFileOnlyCategory_doesNotSaveAuditLog() {
        AuditEvent event = AuditEvent.of(
                AuditCategory.AUTH_LOGIN,
                "LOGIN_FAILED",
                "MEMBER",
                null,
                "MEMBER",
                null,
                AuditResult.FAILURE,
                Map.of("reason", "INVALID_PASSWORD")
        );

        auditLogAdapter.record(event);

        verify(auditLogJpaRepository, never()).save(any(AuditLogEntity.class));
    }

    @Test
    @DisplayName("감사 로그 DB 저장 실패는 호출부로 전파하지 않는다")
    void record_whenSaveFails_doesNotThrow() {
        when(auditLogJpaRepository.save(any(AuditLogEntity.class)))
                .thenThrow(new RuntimeException("db down"));
        AuditEvent event = AuditEvent.of(
                AuditCategory.REPORT,
                "REPORT_RECEIVED",
                "MEMBER",
                "1",
                "REPORT",
                "10",
                AuditResult.SUCCESS,
                Map.of()
        );

        auditLogAdapter.record(event);

        verify(auditLogJpaRepository).save(any(AuditLogEntity.class));
    }

    @Test
    @DisplayName("활성 트랜잭션 동기화가 있으면 커밋 이후에 audit_logs에 저장한다")
    void record_withTransactionSynchronization_savesAfterCommit() {
        AuditEvent event = AuditEvent.of(
                AuditCategory.REPORT,
                "REPORT_RECEIVED",
                "MEMBER",
                "1",
                "REPORT",
                "10",
                AuditResult.SUCCESS,
                Map.of()
        );

        TransactionSynchronizationManager.initSynchronization();
        try {
            auditLogAdapter.record(event);

            verify(auditLogJpaRepository, never()).save(any(AuditLogEntity.class));

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCommit());

            verify(auditLogJpaRepository).save(any(AuditLogEntity.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
