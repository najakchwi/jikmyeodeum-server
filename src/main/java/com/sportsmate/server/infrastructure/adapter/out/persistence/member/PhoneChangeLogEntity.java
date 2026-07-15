package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "phone_change_logs")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneChangeLogEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "old_phone", length = 20, nullable = false)
    private String oldPhone;

    @Column(name = "new_phone", length = 20, nullable = false)
    private String newPhone;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
