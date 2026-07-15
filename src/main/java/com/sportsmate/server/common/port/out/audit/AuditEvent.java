package com.sportsmate.server.common.port.out.audit;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditEvent(
        AuditCategory category,
        String action,
        String actorType,
        String actorId,
        String targetType,
        String targetId,
        AuditResult result,
        Map<String, Object> detail,
        LocalDateTime occurredAt
) {

    public static AuditEvent of(
            AuditCategory category,
            String action,
            String actorType,
            String actorId,
            String targetType,
            String targetId,
            AuditResult result,
            Map<String, Object> detail
    ) {
        return new AuditEvent(
                category,
                action,
                actorType,
                actorId,
                targetType,
                targetId,
                result,
                detail,
                LocalDateTime.now()
        );
    }
}
