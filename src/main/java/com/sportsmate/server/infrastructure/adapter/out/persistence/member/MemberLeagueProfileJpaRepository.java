package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberLeagueProfileJpaRepository
        extends JpaRepository<MemberLeagueProfileEntity, MemberLeagueProfileId> {
    List<MemberLeagueProfileEntity> findAllByIdMemberId(Long memberId);
    void deleteAllByIdMemberId(Long memberId);
}
