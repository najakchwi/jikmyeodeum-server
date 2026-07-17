package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.persistence.BaseTimeEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "member_league_watch_styles")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberLeagueWatchStyleEntity extends BaseTimeEntity implements Persistable<MemberLeagueWatchStyleId> {

    @EmbeddedId
    private MemberLeagueWatchStyleId id;

    @Override
    public boolean isNew() {
        return true;
    }
}
