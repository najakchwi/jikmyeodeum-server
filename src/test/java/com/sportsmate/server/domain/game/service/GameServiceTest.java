package com.sportsmate.server.domain.game.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.game.Game;
import com.sportsmate.server.domain.game.Team;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.domain.game.port.out.TeamOutPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GameService 단위 테스트")
class GameServiceTest {

    private final FakeGameOutPort gameOutPort = new FakeGameOutPort();
    private final FakeApplicationOutPort applicationOutPort = new FakeApplicationOutPort();
    private final FakeTeamOutPort teamOutPort = new FakeTeamOutPort();
    private final GameService gameService = new GameService(gameOutPort, applicationOutPort, teamOutPort);

    @Test
    @DisplayName("경기 조회 응답의 신청 인원은 대기 중인 신청만 집계한다")
    void game_usesWaitingApplicationCountAsApplicantCount() {
        gameOutPort.game = Optional.of(new Game(
                "10",
                "LG",
                "KT",
                "잠실",
                LocalDate.now().plusDays(1),
                LocalTime.of(18, 30),
                LocalDate.now(),
                null,
                null,
                null,
                null,
                1L,
                2L));
        gameOutPort.totalApplicationCount = 4;
        gameOutPort.waitingApplicationCount = 2;

        var result = gameService.game(1L, "10");

        assertThat(result.applicantCount()).isEqualTo(2);
        assertThat(gameOutPort.countedWaitingGameId).isEqualTo("10");
        assertThat(gameOutPort.countedAllGameId).isNull();
    }

    private static class FakeGameOutPort implements GameOutPort {
        private Optional<Game> game = Optional.empty();
        private long totalApplicationCount;
        private long waitingApplicationCount;
        private String countedAllGameId;
        private String countedWaitingGameId;

        @Override
        public List<Game> findBetween(LocalDate startDate, LocalDate endDate) {
            return List.of();
        }

        @Override
        public Optional<SeasonRange> findSeasonRange() {
            return Optional.empty();
        }

        @Override
        public List<Game> findByDate(LocalDate date) {
            return game.stream().toList();
        }

        @Override
        public Optional<Game> findById(String id) {
            return game;
        }

        @Override
        public long countApplications(String gameId) {
            countedAllGameId = gameId;
            return totalApplicationCount;
        }

        @Override
        public long countWaitingApplications(String gameId) {
            countedWaitingGameId = gameId;
            return waitingApplicationCount;
        }

        @Override
        public UpsertResult upsertAll(List<GameSyncCommand> commands) {
            return new UpsertResult(0, 0, List.of());
        }

        @Override
        public int cancelMissingSyncedGames(YearMonth month, List<String> fetchedKboGameIds) {
            return 0;
        }
    }

    private static class FakeApplicationOutPort implements ApplicationOutPort {
        @Override
        public Application save(Application application) {
            return application;
        }

        @Override
        public String createMatch(Application application, Application opponent) {
            return "1";
        }

        @Override
        public String createSoloMatch(Application application) {
            return "1";
        }

        @Override
        public void addParticipant(String chatId, Application application) {
        }

        @Override
        public Optional<Application> findByIdAndMemberId(String id, Long memberId) {
            return Optional.empty();
        }

        @Override
        public Optional<Application> findByMemberIdAndGameId(Long memberId, String gameId) {
            return Optional.empty();
        }

        @Override
        public Optional<Application> findByChatIdAndMemberId(String chatId, Long memberId) {
            return Optional.empty();
        }

        @Override
        public List<Application> findByMemberId(Long memberId) {
            return List.of();
        }

        @Override
        public List<Application> findWaitingByGameId(String gameId) {
            return List.of();
        }

        @Override
        public List<String> findGameIdsWithWaitingApplications() {
            return List.of();
        }

        @Override
        public boolean existsActiveByMemberIdAndGameId(Long memberId, String gameId) {
            return false;
        }

        @Override
        public List<LocalDate> findAppliedDates(Long memberId, int year, int month) {
            return List.of();
        }

        @Override
        public long countChattingCancellationsSince(Long memberId, LocalDateTime since) {
            return 0;
        }
    }

    private static class FakeTeamOutPort implements TeamOutPort {
        @Override
        public List<Team> findAll() {
            return List.of();
        }

        @Override
        public Optional<Team> findByKboCode(String kboCode) {
            return Optional.empty();
        }

        @Override
        public Optional<Team> findByShortName(String shortName) {
            return Optional.empty();
        }
    }
}
