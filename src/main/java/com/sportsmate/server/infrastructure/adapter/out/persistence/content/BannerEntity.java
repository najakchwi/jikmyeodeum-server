package com.sportsmate.server.infrastructure.adapter.out.persistence.content;

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
@Table(name = "banners")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BannerEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "image_key", length = 255, nullable = false)
    private String imageKey;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;
}
