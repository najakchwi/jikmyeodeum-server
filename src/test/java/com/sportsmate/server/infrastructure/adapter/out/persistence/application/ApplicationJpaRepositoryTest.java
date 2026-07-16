package com.sportsmate.server.infrastructure.adapter.out.persistence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
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

    @Autowired
    EntityManager entityManager;

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

    @Test
    @DisplayName("동일 회원·경기에는 cancelled를 제외한 활성 신청을 1건만 저장할 수 있다")
    void activeApplicationUniqueIndex_blocksDuplicateActiveApplication() {
        entityManager.createNativeQuery("""
                ALTER TABLE match_applications
                    ADD COLUMN IF NOT EXISTS active_unique_key BIGINT
                    GENERATED ALWAYS AS (CASE WHEN status <> 'cancelled' THEN 1 ELSE NULL END)
                """).executeUpdate();
        entityManager.createNativeQuery("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_match_applications_active_member_game_test
                    ON match_applications(member_id, game_id, active_unique_key)
                """).executeUpdate();
        repository.saveAndFlush(application(30L, 1L, "cancelled"));
        repository.saveAndFlush(application(30L, 1L, "waiting"));

        assertThatThrownBy(() -> repository.saveAndFlush(application(30L, 1L, "matched")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("동일 회원·날짜에는 cancelled를 제외한 활성 신청을 1건만 저장할 수 있다")
    void activeApplicationDateUniqueIndex_blocksDuplicateActiveApplicationOnSameDate() {
        entityManager.createNativeQuery("""
                ALTER TABLE match_applications
                    ADD COLUMN IF NOT EXISTS active_unique_key BIGINT
                    GENERATED ALWAYS AS (CASE WHEN status <> 'cancelled' THEN 1 ELSE NULL END)
                """).executeUpdate();
        entityManager.createNativeQuery("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_match_applications_active_member_date_test
                    ON match_applications(member_id, game_date, active_unique_key)
                """).executeUpdate();
        LocalDate gameDate = LocalDate.of(2026, 7, 16);
        repository.saveAndFlush(application(30L, 1L, gameDate, "cancelled"));
        repository.saveAndFlush(application(40L, 1L, gameDate, "waiting"));

        assertThatThrownBy(() -> repository.saveAndFlush(application(50L, 1L, gameDate, "matched")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("회원의 특정 날짜 활성 신청 존재 여부는 cancelled를 제외하고 조회한다")
    void existsByMemberIdAndGameDateAndStatusNot_ignoresCancelledApplication() {
        LocalDate gameDate = LocalDate.of(2026, 7, 16);
        repository.saveAndFlush(application(30L, 1L, gameDate, "cancelled"));

        boolean cancelledOnly = repository.existsByMemberIdAndGameDateAndStatusNot(1L, gameDate, "cancelled");
        repository.saveAndFlush(application(40L, 1L, gameDate, "chatting"));
        boolean activeExists = repository.existsByMemberIdAndGameDateAndStatusNot(1L, gameDate, "cancelled");

        assertThat(cancelledOnly).isFalse();
        assertThat(activeExists).isTrue();
    }

    @Test
    @DisplayName("신청 엔티티는 낙관적 락 버전 컬럼을 가진다")
    void applicationEntity_hasVersionColumn() {
        ApplicationEntity saved = repository.saveAndFlush(application(40L, 1L, "waiting"));

        assertThat(saved.getVersion()).isNotNull();
    }

    private ApplicationEntity application(Long gameId, Long memberId, String status) {
        return application(gameId, memberId, LocalDate.of(2026, 7, 16), status);
    }

    private ApplicationEntity application(Long gameId, Long memberId, LocalDate gameDate, String status) {
        return ApplicationEntity.builder()
                .memberId(memberId)
                .gameId(gameId)
                .gameDate(gameDate)
                .partySize(1)
                .status(status)
                .appliedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
