package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_preferences")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPreferenceEntity extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "gender_pref", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ANY'")
    @Enumerated(EnumType.STRING)
    private GenderPref genderPref;

    @Column(name = "age_pref", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ANY'")
    @Enumerated(EnumType.STRING)
    private AgePref agePref;

    @Column(name = "smoking_pref", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ANY'")
    @Enumerated(EnumType.STRING)
    private SmokingPref smokingPref;

    @Column(name = "distance_km", nullable = false, columnDefinition = "INTEGER DEFAULT 5")
    private Integer distanceKm;
}
