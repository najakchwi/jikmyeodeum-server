package com.sportsmate.server.common.port.out.audit;

public interface AuditLogPort {
    void record(AuditEvent event);
}
