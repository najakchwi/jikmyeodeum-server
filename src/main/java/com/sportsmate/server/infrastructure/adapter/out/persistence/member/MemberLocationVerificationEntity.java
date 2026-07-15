package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_location_verifications")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberLocationVerificationEntity extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
