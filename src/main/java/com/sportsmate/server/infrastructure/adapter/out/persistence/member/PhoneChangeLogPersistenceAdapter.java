package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.member.port.out.PhoneChangeLogPort;
import java.time.LocalDateTime;
import java.util.Optional;

@PersistenceAdapter
public class PhoneChangeLogPersistenceAdapter implements PhoneChangeLogPort {

    private final PhoneChangeLogJpaRepository repository;

    public PhoneChangeLogPersistenceAdapter(PhoneChangeLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(PhoneChangeLog log) {
        repository.save(PhoneChangeLogEntity.builder()
                .memberId(log.memberId())
                .oldPhone(log.oldPhone())
                .newPhone(log.newPhone())
                .changedAt(log.changedAt())
                .build());
    }

    @Override
    public Optional<PhoneChangeLog> findLatestByMemberIdSince(Long memberId, LocalDateTime since) {
        return repository.findFirstByMemberIdAndChangedAtGreaterThanEqualOrderByChangedAtDesc(memberId, since)
                .map(entity -> new PhoneChangeLog(
                        entity.getMemberId(),
                        entity.getOldPhone(),
                        entity.getNewPhone(),
                        entity.getChangedAt()));
    }
}
