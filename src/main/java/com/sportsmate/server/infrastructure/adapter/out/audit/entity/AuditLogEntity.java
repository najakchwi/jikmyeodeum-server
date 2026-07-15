package com.sportsmate.server.infrastructure.adapter.out.audit.entity;

import com.sportsmate.server.common.port.out.audit.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "category", length = 30, nullable = false)
    private String category;

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(name = "actor_type", length = 20, nullable = false)
    private String actorType;

    @Column(name = "actor_id", length = 50)
    private String actorId;

    @Column(name = "target_type", length = 30)
    private String targetType;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(name = "result", length = 10, nullable = false)
    private String result;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AuditLogEntity from(AuditEvent event, String detailJson) {
        return AuditLogEntity.builder()
                .category(event.category().name())
                .action(event.action())
                .actorType(event.actorType())
                .actorId(event.actorId())
                .targetType(event.targetType())
                .targetId(event.targetId())
                .result(event.result().name())
                .detail(detailJson)
                .occurredAt(event.occurredAt())
                .build();
    }
}
