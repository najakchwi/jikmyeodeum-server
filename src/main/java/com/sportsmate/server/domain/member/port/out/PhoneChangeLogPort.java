package com.sportsmate.server.domain.member.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PhoneChangeLogPort {

    void save(PhoneChangeLog log);

    Optional<PhoneChangeLog> findLatestByMemberIdSince(Long memberId, LocalDateTime since);

    record PhoneChangeLog(
            Long memberId,
            String oldPhone,
            String newPhone,
            LocalDateTime changedAt) {}
}
