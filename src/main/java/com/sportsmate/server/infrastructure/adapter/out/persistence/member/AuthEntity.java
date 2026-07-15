package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

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
@Table(name = "auth")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "login_type", length = 20, nullable = false)
    private String loginType;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;
}
