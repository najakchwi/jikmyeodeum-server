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
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "terms")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "version", length = 20, nullable = false)
    private String version;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "content_key", length = 255)
    private String contentKey;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "required", nullable = false)
    private Boolean required;

    @Column(name = "valid_days")
    private Integer validDays;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;
}
