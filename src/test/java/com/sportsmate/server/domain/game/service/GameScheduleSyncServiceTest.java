package com.sportsmate.server.domain.game.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.domain.Event;
import com.sportsmate.server.common.port.out.event.EventPublisher;
import com.sportsmate.server.domain.game.event.GameRescheduledEvent;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.domain.game.port.out.GameScheduleSourcePort;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GameScheduleSyncService 단위 테스트")
class GameScheduleSyncServiceTest {

    private final FakeGameScheduleSourcePort scheduleSourcePort = new FakeGameScheduleSourcePort();
    private final FakeGameOutPort gameOutPort = new FakeGameOutPort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final GameScheduleSyncService service = new GameScheduleSyncService(
            scheduleSourcePort,
            gameOutPort,
            eventPublisher
    );

    @Test
    @DisplayName("월별 스케줄 동기화 시 원천 데이터를 upsert하고 결과를 반환한다")
    void sync_withSourceGames_upsertsGames() {
        scheduleSourcePort.games = List.of(
                new GameScheduleSourcePort.SourceGame(
                        "20260701LGHH0",
                        1L,
                        8L,
                        1L,
                        LocalDate.of(2026, 7, 1),
                        LocalTime.of(18, 30),
                        null,
                        null
                )
        );
        gameOutPort.upsertResult = new GameOutPort.UpsertResult(
                1,
                0,
                List.of(new GameRescheduledEvent(
                        "1",
                        LocalDate.of(2026, 7, 1),
                        LocalTime.of(18, 0),
                        LocalDate.of(2026, 7, 1),
                        LocalTime.of(18, 30)
                ))
        );
        gameOutPort.cancelled = 2;

        var result = service.sync(YearMonth.of(2026, 7));

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.cancelled()).isEqualTo(2);
        assertThat(gameOutPort.commands).hasSize(1);
        assertThat(gameOutPort.commands.getFirst().status()).isEqualTo("SCHEDULED");
        assertThat(gameOutPort.commands.getFirst().homeTeamId()).isEqualTo(1L);
        assertThat(gameOutPort.commands.getFirst().awayTeamId()).isEqualTo(8L);
        assertThat(gameOutPort.commands.getFirst().stadiumId()).isEqualTo(1L);
        assertThat(gameOutPort.fetchedKboGameIds).containsExactly("20260701LGHH0");
        assertThat(gameOutPort.month).isEqualTo(YearMonth.of(2026, 7));
        assertThat(eventPublisher.events).hasOnlyElementsOfType(GameRescheduledEvent.class);
    }

    @Test
    @DisplayName("외부 소스에 양쪽 점수가 모두 있으면 종료(FINISHED) 상태로 동기화한다")
    void sync_withBothScores_marksGameFinished() {
        scheduleSourcePort.games = List.of(
                new GameScheduleSourcePort.SourceGame(
                        "20260701LGHH0",
                        1L,
                        8L,
                        1L,
                        LocalDate.of(2026, 7, 1),
                        LocalTime.of(18, 30),
                        5,
                        3
                )
        );

        service.sync(YearMonth.of(2026, 7));

        assertThat(gameOutPort.commands.getFirst().status()).isEqualTo("FINISHED");
    }

    @Test
    @DisplayName("월 범위 스케줄 동기화 시 시작 월부터 종료 월까지 순서대로 동기화하고 결과를 합산한다")
    void sync_withMonthRange_syncsEachMonthAndAggregatesResults() {
        gameOutPort.upsertResult = new GameOutPort.UpsertResult(1, 2, List.of());
        gameOutPort.cancelled = 3;

        var result = service.sync(YearMonth.of(2026, 6), YearMonth.of(2026, 9));

        assertThat(scheduleSourcePort.requestedMonths).containsExactly(
                YearMonth.of(2026, 6),
                YearMonth.of(2026, 7),
                YearMonth.of(2026, 8),
                YearMonth.of(2026, 9)
        );
        assertThat(result.created()).isEqualTo(4);
        assertThat(result.updated()).isEqualTo(8);
        assertThat(result.cancelled()).isEqualTo(12);
        assertThat(result.months()).hasSize(4);
        assertThat(result.months().getFirst().month()).isEqualTo(YearMonth.of(2026, 6));
    }

    private static class FakeGameScheduleSourcePort implements GameScheduleSourcePort {
        private List<SourceGame> games = List.of();
        private final List<YearMonth> requestedMonths = new ArrayList<>();

        @Override
        public List<SourceGame> fetchMonthlySchedule(YearMonth month) {
            requestedMonths.add(month);
            return games;
        }
    }

    private static class FakeGameOutPort implements GameOutPort {
        private List<GameSyncCommand> commands = List.of();
        private List<String> fetchedKboGameIds = List.of();
        private YearMonth month;
        private UpsertResult upsertResult = new UpsertResult(0, 0, List.of());
        private int cancelled;

        @Override
        public List<com.sportsmate.server.domain.game.Game> findBetween(LocalDate startDate, LocalDate endDate) {
            return List.of();
        }

        @Override
        public Optional<SeasonRange> findSeasonRange() {
            return Optional.empty();
        }

        @Override
        public List<com.sportsmate.server.domain.game.Game> findByDate(LocalDate date) {
            return List.of();
        }

        @Override
        public Optional<com.sportsmate.server.domain.game.Game> findById(String id) {
            return Optional.empty();
        }

        @Override
        public long countApplications(String gameId) {
            return 0;
        }

        @Override
        public long countWaitingApplications(String gameId) {
            return 0;
        }

        @Override
        public UpsertResult upsertAll(List<GameSyncCommand> commands) {
            this.commands = commands;
            return upsertResult;
        }

        @Override
        public int cancelMissingSyncedGames(YearMonth month, List<String> fetchedKboGameIds) {
            this.month = month;
            this.fetchedKboGameIds = fetchedKboGameIds;
            return cancelled;
        }
    }

    private static class RecordingEventPublisher implements EventPublisher {
        private final List<Event> events = new ArrayList<>();

        @Override
        public void publish(Event event) {
            events.add(event);
        }
    }
}
