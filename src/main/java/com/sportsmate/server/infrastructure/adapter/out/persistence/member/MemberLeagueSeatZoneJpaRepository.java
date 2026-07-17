package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberLeagueSeatZoneJpaRepository
        extends JpaRepository<MemberLeagueSeatZoneEntity, MemberLeagueSeatZoneId> {
    List<MemberLeagueSeatZoneEntity> findAllByIdMemberId(Long memberId);
    List<MemberLeagueSeatZoneEntity> findAllByIdMemberIdAndIdLeagueId(Long memberId, Long leagueId);
    void deleteAllByIdMemberId(Long memberId);
    void deleteAllByIdMemberIdAndIdLeagueId(Long memberId, Long leagueId);
}
