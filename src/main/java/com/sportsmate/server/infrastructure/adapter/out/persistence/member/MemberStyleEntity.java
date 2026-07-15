package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
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
@Table(name = "member_styles")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStyleEntity extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "favorite_team_id")
    private Long favoriteTeamId;

    @Column(name = "personality", length = 50)
    @Enumerated(EnumType.STRING)
    private Personality personality;

    @Column(name = "talk_style", length = 50)
    @Enumerated(EnumType.STRING)
    private TalkStyle talkStyle;

    @Column(name = "smoking_status", length = 50)
    @Enumerated(EnumType.STRING)
    private SmokingStatus smokingStatus;
}
