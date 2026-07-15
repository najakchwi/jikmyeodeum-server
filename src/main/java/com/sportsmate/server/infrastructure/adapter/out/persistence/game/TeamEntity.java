package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

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
@Table(name = "teams")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sport_id", nullable = false)
    private Long sportId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "short_name", length = 20, unique = true)
    private String shortName;

    @Column(name = "kbo_code", length = 20, unique = true)
    private String kboCode;

    @Column(name = "emblem_image_key", length = 255)
    private String emblemImageKey;

    @Column(name = "primary_color_hex", length = 7)
    private String primaryColorHex;
}
