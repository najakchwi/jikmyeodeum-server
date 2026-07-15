package com.sportsmate.server.infrastructure.adapter.out.persistence.content;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faqs")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "question", length = 255, nullable = false)
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
