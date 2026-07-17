package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.persistence.BaseTimeEntity;
import com.sportsmate.server.domain.member.enums.FanLevel;
import com.sportsmate.server.domain.member.enums.TeamPref;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "member_league_profiles")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberLeagueProfileEntity extends BaseTimeEntity {

    @EmbeddedId
    private MemberLeagueProfileId id;

    @Column(name = "favorite_team_id")
    private Long favoriteTeamId;

    @Column(name = "team_pref", length = 20)
    @Enumerated(EnumType.STRING)
    private TeamPref teamPref;

    @Column(name = "fan_level", length = 50)
    @Enumerated(EnumType.STRING)
    private FanLevel fanLevel;
}
