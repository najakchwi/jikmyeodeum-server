package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.member.port.out.MemberWithdrawalLogPort;

@PersistenceAdapter
public class MemberWithdrawalLogPersistenceAdapter implements MemberWithdrawalLogPort {

    private final MemberWithdrawalLogJpaRepository repository;

    public MemberWithdrawalLogPersistenceAdapter(MemberWithdrawalLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(WithdrawalLog log) {
        repository.save(MemberWithdrawalLogEntity.builder()
                .memberId(log.memberId())
                .phone(log.phone())
                .nickname(log.nickname())
                .reason(log.reason().name())
                .reasonDetail(log.reasonDetail())
                .withdrawnAt(log.withdrawnAt())
                .build());
    }
}
