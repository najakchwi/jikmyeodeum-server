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
@Table(name = "members")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "auth_id")
    private Long authId;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "nickname", length = 50, nullable = false)
    private String nickname;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "profile_image_key", length = 255)
    private String profileImageKey;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "expo_push_token", length = 255)
    private String expoPushToken;

    @Column(name = "welcome_notified", nullable = false)
    private boolean welcomeNotified;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
