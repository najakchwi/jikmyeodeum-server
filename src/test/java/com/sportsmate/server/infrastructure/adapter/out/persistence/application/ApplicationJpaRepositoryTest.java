package com.sportsmate.server.infrastructure.adapter.out.persistence.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ApplicationJpaRepository JPA 테스트")
class ApplicationJpaRepositoryTest {

    @Autowired
    ApplicationJpaRepository repository;

    @Test
    @DisplayName("경기별 상태 카운트는 지정한 상태의 신청만 집계한다")
    void countByGameIdAndStatus_countsOnlyMatchingStatus() {
        repository.save(application(10L, 1L, "waiting"));
        repository.save(application(10L, 2L, "waiting"));
        repository.save(application(10L, 3L, "matched"));
        repository.save(application(10L, 4L, "cancelled"));
        repository.save(application(20L, 5L, "waiting"));

        long waitingCount = repository.countByGameIdAndStatus(10L, "waiting");

        assertThat(waitingCount).isEqualTo(2);
    }

    private ApplicationEntity application(Long gameId, Long memberId, String status) {
        return ApplicationEntity.builder()
                .memberId(memberId)
                .gameId(gameId)
                .partySize(1)
                .status(status)
                .appliedAt(LocalDateTime.now())
                .build();
    }
}
