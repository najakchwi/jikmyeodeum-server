package com.sportsmate.server.infrastructure.adapter.out.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.common.port.out.audit.AuditEvent;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.infrastructure.adapter.out.audit.entity.AuditLogEntity;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@PersistenceAdapter
@RequiredArgsConstructor
public class AuditLogAdapter implements AuditLogPort {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void record(AuditEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    writeAndPersist(event);
                }
            });
            return;
        }
        writeAndPersist(event);
    }

    private void writeAndPersist(AuditEvent event) {
        String detailJson = writeDetail(event.detail());

        AUDIT_LOG.info(
                "category={} action={} actor={}:{} target={}:{} result={} detail={}",
                event.category(),
                event.action(),
                event.actorType(),
                event.actorId(),
                event.targetType(),
                event.targetId(),
                event.result(),
                detailJson
        );

        if (event.category().isPersisted()) {
            persist(event, detailJson);
        }
    }

    private void persist(AuditEvent event, String detailJson) {
        try {
            auditLogJpaRepository.save(AuditLogEntity.from(event, detailJson));
        } catch (Exception e) {
            AUDIT_LOG.error(
                    "Failed to persist audit log. category={} action={}",
                    event.category(),
                    event.action(),
                    e
            );
        }
    }

    private String writeDetail(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail == null ? Collections.emptyMap() : detail);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
