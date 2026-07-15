package com.sportsmate.server.infrastructure.adapter.out.audit;

import com.sportsmate.server.infrastructure.adapter.out.audit.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {
}
