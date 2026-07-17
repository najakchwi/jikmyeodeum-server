package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberLeagueWatchStyleJpaRepository
        extends JpaRepository<MemberLeagueWatchStyleEntity, MemberLeagueWatchStyleId> {
    List<MemberLeagueWatchStyleEntity> findAllByIdMemberId(Long memberId);
    List<MemberLeagueWatchStyleEntity> findAllByIdMemberIdAndIdLeagueId(Long memberId, Long leagueId);
    void deleteAllByIdMemberId(Long memberId);
    void deleteAllByIdMemberIdAndIdLeagueId(Long memberId, Long leagueId);
}
