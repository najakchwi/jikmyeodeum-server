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
@Table(name = "member_league_seat_zones")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberLeagueSeatZoneEntity extends BaseTimeEntity implements Persistable<MemberLeagueSeatZoneId> {

    @EmbeddedId
    private MemberLeagueSeatZoneId id;

    @Override
    public boolean isNew() {
        return true;
    }
}
