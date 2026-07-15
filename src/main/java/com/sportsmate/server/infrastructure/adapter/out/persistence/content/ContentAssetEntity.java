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
@Table(name = "content_assets")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentAssetEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "code", length = 80, nullable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "object_key", length = 255, nullable = false)
    private String objectKey;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
