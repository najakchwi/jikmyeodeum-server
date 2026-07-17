package com.sportsmate.server.domain.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.exception.ApplicationErrorCode;
import com.sportsmate.server.domain.application.matching.GreedyMatchingEngine;
import com.sportsmate.server.domain.application.matching.MatchReason;
import com.sportsmate.server.domain.application.matching.MatchCandidateFactory;
import com.sportsmate.server.domain.application.matching.MatchWeights;
import com.sportsmate.server.domain.application.matching.filter.AgePreferenceFilter;
import com.sportsmate.server.domain.application.matching.filter.DistanceFilter;
import com.sportsmate.server.domain.application.matching.filter.GenderPreferenceFilter;
import com.sportsmate.server.domain.application.matching.filter.NotPreviouslyRejectedFilter;
import com.sportsmate.server.domain.application.matching.filter.SmokingPreferenceFilter;
import com.sportsmate.server.domain.application.matching.scorer.DistanceScorer;
import com.sportsmate.server.domain.application.matching.scorer.PersonalityScorer;
import com.sportsmate.server.domain.application.matching.scorer.TeamScorer;
import com.sportsmate.server.domain.application.matching.scorer.TrustScoreScorer;
import com.sportsmate.server.domain.application.matching.scorer.WatchStyleScorer;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.chat.port.in.ChatUseCase;
import com.sportsmate.server.domain.game.Game;
import com.sportsmate.server.domain.game.Team;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.domain.game.port.out.TeamOutPort;
import com.sportsmate.server.domain.game.service.GameService;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.MemberLeagueProfile;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.exception.MemberErrorCode;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import com.sportsmate.server.domain.member.port.in.MemberUseCase;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.notification.port.in.NotificationUseCase;
import com.sportsmate.server.domain.review.port.dto.ReviewDetail;
import com.sportsmate.server.domain.review.port.out.ReviewOutPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@DisplayName("ApplicationService 단위 테스트")
class ApplicationServiceTest {

    private final FakeApplicationOutPort applicationOutPort = new FakeApplicationOutPort();
    private final FakeGameOutPort gameOutPort = new FakeGameOutPort();
    private final FakeTeamOutPort teamOutPort = new FakeTeamOutPort();
    private final GameService gameService = new GameService(gameOutPort, applicationOutPort, teamOutPort);
    private final FakeMemberUseCase memberUseCase = new FakeMemberUseCase();
    private final FakeMemberOutPort memberOutPort = new FakeMemberOutPort();
    private final FakeNotificationUseCase notificationUseCase = new FakeNotificationUseCase();
    private final FakeChatUseCase chatUseCase = new FakeChatUseCase();
    private final FakeReviewOutPort reviewOutPort = new FakeReviewOutPort();
    private final GreedyMatchingEngine matchingEngine = new GreedyMatchingEngine(
            List.of(new GenderPreferenceFilter(), new AgePreferenceFilter(), new SmokingPreferenceFilter(),
                    new DistanceFilter(), new NotPreviouslyRejectedFilter()),
            List.of(new TrustScoreScorer(), new TeamScorer(), new WatchStyleScorer(),
                    new PersonalityScorer(), new DistanceScorer()),
            List.of(new TrustScoreScorer()));
    private final MatchCandidateFactory matchCandidateFactory = new MatchCandidateFactory();
    private final MatchWeights matchWeights = new MatchWeights(Map.of(
            "trust", 20.0,
            "team", 40.0,
            "watchStyle", 20.0,
            "personality", 10.0,
            "distance", 10.0), 60);
    private final ApplicationMatchingBatchProcessor matchingBatchProcessor = new ApplicationMatchingBatchProcessor(
            applicationOutPort, gameOutPort, memberUseCase, notificationUseCase, matchingEngine,
            matchCandidateFactory, matchWeights);
    private final ApplicationService applicationService = new ApplicationService(
            applicationOutPort, gameOutPort, gameService, memberUseCase, memberOutPort,
            notificationUseCase, chatUseCase, reviewOutPort, matchingBatchProcessor);

    @Test
    @DisplayName("매칭된 신청을 취소하면 상대 신청은 대기 상태로 돌아가고 상대에게 알림을 보낸 뒤 취소자는 신뢰도가 즉시 차감된다")
    void cancel_matchedApplication_resetsOpponentToWaitingNotifiesAndAppliesPenalty() {
        Application mine = Application.create("1", 1L, "10");
        Application opponent = Application.create("2", 2L, "10");
        mine.assign(2L);
        opponent.assign(1L);
        opponent.markAccepted();
        applicationOutPort.save(mine);
        applicationOutPort.save(opponent);
        memberOutPort.members.put(1L, member(1L, 100));

        applicationService.cancel(1L, "1");

        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("cancelled");
        Application resetOpponent = applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow();
        assertThat(resetOpponent.getStatus()).isEqualTo("waiting");
        assertThat(resetOpponent.getMatchedMemberId()).isNull();
        assertThat(resetOpponent.getMatchedAt()).isNull();
        assertThat(resetOpponent.getExpiresAt()).isNull();
        assertThat(resetOpponent.getResponse()).isNull();
        assertThat(notificationUseCase.pushed).containsExactly("2:매칭이 취소됐어요");
        assertThat(memberOutPort.members.get(1L).getTrustScore()).isEqualTo(97);
    }

    @Test
    @DisplayName("매칭된 신청을 수락하면 상대 응답과 무관하게 채팅방 ID를 저장한다")
    void accept_matchedApplication_savesCreatedMatchIdAsChatId() {
        Application mine = Application.create("1", 1L, "10");
        Application opponent = Application.create("2", 2L, "10");
        mine.assign(2L);
        opponent.assign(1L);
        applicationOutPort.save(mine);
        applicationOutPort.save(opponent);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        var result = applicationService.accept(1L, "1");

        assertThat(result.chatId()).isEqualTo("30");
        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getChatId())
                .isEqualTo("30");
        assertThat(applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow().getChatId())
                .isNull();
        assertThat(applicationOutPort.createdMatches).containsExactly("solo:1");
        assertThat(chatUseCase.systemMessages).containsExactly("30:member1님이 채팅에 참여했어요.");
    }

    @Test
    @DisplayName("수락 저장 중 낙관적 락 충돌이 발생하면 매칭 준비 전 상태로 응답한다")
    void accept_whenOptimisticLockFails_throwsMatchNotReady() {
        Application mine = Application.create("1", 1L, "10");
        Application opponent = Application.create("2", 2L, "10");
        mine.assign(2L);
        opponent.assign(1L);
        applicationOutPort.save(mine);
        applicationOutPort.save(opponent);
        applicationOutPort.failOnSaveAndFlushOptimisticLock = true;

        assertThatThrownBy(() -> applicationService.accept(1L, "1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.MATCH_NOT_READY);
    }

    @Test
    @DisplayName("채팅 중 매칭을 취소하면 상대 신청도 취소하고 반복 취소자는 신뢰도를 차감한다")
    void cancel_chattingApplication_cancelsOpponentAndAppliesPenalty() {
        Application mine = Application.create("1", 1L, "10");
        Application opponent = Application.create("2", 2L, "10");
        mine.assign(2L);
        opponent.assign(1L);
        mine.confirm("30");
        opponent.confirm("30");
        applicationOutPort.save(mine);
        applicationOutPort.save(opponent);
        applicationOutPort.chattingCancellationCount = 3;
        memberOutPort.members.put(1L, member(1L, 100));

        applicationService.cancel(1L, "1");

        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("cancelled");
        assertThat(applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow().getStatus())
                .isEqualTo("cancelled");
        assertThat(chatUseCase.systemMessages)
                .containsExactly("30:member1님이 매칭을 취소했어요. 채팅방이 종료됩니다.");
        assertThat(memberOutPort.members.get(1L).getTrustScore()).isEqualTo(90);
    }

    @Test
    @DisplayName("탈퇴 시 대기 중인 신청은 그냥 취소된다")
    void cancelAllActiveByMember_waitingApplication_cancelledWithoutOpponent() {
        applicationOutPort.save(Application.create("1", 1L, "10"));

        applicationService.cancelAllActiveByMember(1L);

        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("cancelled");
    }

    @Test
    @DisplayName("탈퇴 시 매칭된(채팅 시작 전) 신청을 취소하면 상대는 대기 상태로 돌아가고 알림을 받는다")
    void cancelAllActiveByMember_matchedApplication_resetsOpponentAndNotifies() {
        Application mine = Application.create("1", 1L, "10");
        Application opponent = Application.create("2", 2L, "10");
        mine.assign(2L);
        opponent.assign(1L);
        applicationOutPort.save(mine);
        applicationOutPort.save(opponent);

        applicationService.cancelAllActiveByMember(1L);

        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("cancelled");
        Application resetOpponent = applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow();
        assertThat(resetOpponent.getStatus()).isEqualTo("waiting");
        assertThat(resetOpponent.getMatchedMemberId()).isNull();
        assertThat(notificationUseCase.pushed).containsExactly("2:매칭이 취소됐어요");
    }

    @Test
    @DisplayName("탈퇴 시 채팅 중이던 신청을 취소하면 채팅방을 종료하고 상대를 다시 대기 상태로 돌린다")
    void cancelAllActiveByMember_chattingApplication_endsChatAndRequeuesOpponent() {
        Application mine = Application.create("1", 1L, "10");
        Application opponent = Application.create("2", 2L, "10");
        mine.assign(2L);
        opponent.assign(1L);
        mine.confirm("30");
        opponent.confirm("30");
        applicationOutPort.save(mine);
        applicationOutPort.save(opponent);

        applicationService.cancelAllActiveByMember(1L);

        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("cancelled");
        Application resetOpponent = applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow();
        assertThat(resetOpponent.getStatus()).isEqualTo("waiting");
        assertThat(resetOpponent.getChatId()).isNull();
        assertThat(chatUseCase.systemMessages)
                .containsExactly("30:상대방이 서비스를 탈퇴해 채팅방이 종료됩니다.");
        assertThat(notificationUseCase.pushed).containsExactly("2:매칭이 취소됐어요");
    }

    @Test
    @DisplayName("대기 중인 신청을 조회하면 같은 경기의 다른 대기자 프로필 미리보기를 포함한다")
    void get_waitingApplication_includesOtherWaitingMembersPreview() {
        Application mine = Application.create("1", 1L, "10");
        Application other = Application.create("2", 2L, "10");
        applicationOutPort.save(mine);
        applicationOutPort.save(other);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        var result = applicationService.get(1L, "1");

        assertThat(result.waitingPreview()).hasSize(1);
        assertThat(result.waitingPreview().get(0).nickname()).isEqualTo("member2");
    }

    @Test
    @DisplayName("신청해도 즉시 매칭되지 않고 다음 일괄 매칭 전까지 대기 상태로 유지된다")
    void apply_doesNotInstantlyMatchSecondApplicant() {
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        applicationService.apply(1L, "10");
        var second = applicationService.apply(2L, "10");

        assertThat(second.status()).isEqualTo("waiting");
        assertThat(applicationOutPort.findWaitingByGameId("10")).hasSize(2);
    }

    @Test
    @DisplayName("신뢰도가 50점 미만이면 매칭을 신청할 수 없다")
    void apply_withTrustScoreBelowMinimum_throwsException() {
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        memberUseCase.trustScores.put(1L, 49);

        assertThatThrownBy(() -> applicationService.apply(1L, "10"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.TRUST_SCORE_TOO_LOW);
    }

    @Test
    @DisplayName("신뢰도가 50점이면 매칭을 신청할 수 있다")
    void apply_withMinimumTrustScore_succeeds() {
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        memberUseCase.trustScores.put(1L, 50);

        var result = applicationService.apply(1L, "10");

        assertThat(result.status()).isEqualTo("waiting");
    }

    @Test
    @DisplayName("경기 리그 프로필이 없으면 신청할 수 없다")
    void apply_withoutLeagueProfile_throwsLeagueProfileRequired() {
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null, 2L));
        memberOutPort.missingLeagueProfiles.add("1:2");

        assertThatThrownBy(() -> applicationService.apply(1L, "10"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.LEAGUE_PROFILE_REQUIRED);
    }

    @Test
    @DisplayName("신청을 취소한 뒤 같은 경기에 재신청해도 오류 없이 대기 상태로 신청된다")
    void apply_afterCancel_reappliesToSameGameWithoutError() {
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        var first = applicationService.apply(1L, "10");
        applicationService.cancel(1L, first.id());

        // 취소 후 (member, game)에 cancelled + 새 신청이 공존한다. 재신청 시 get()→GameService.toResult가
        // findByMemberIdAndGameId를 호출하는데, 여기서 NonUniqueResult로 터지지 않아야 한다.
        var reapplied = applicationService.apply(1L, "10");

        assertThat(reapplied.status()).isEqualTo("waiting");
        assertThat(reapplied.id()).isNotEqualTo(first.id());
        assertThat(applicationOutPort.findByMemberIdAndGameId(1L, "10").orElseThrow().getStatus())
                .isEqualTo("waiting");
    }

    @Test
    @DisplayName("같은 날짜의 다른 경기에 대기 신청이 있으면 날짜 중복으로 거절한다")
    void apply_withWaitingApplicationOnSameDate_throwsAlreadyAppliedOnDate() {
        LocalDate gameDate = LocalDate.now().plusDays(1);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", gameDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        gameOutPort.games.put("20", new Game(
                "20", "KIA", "KT", "수원", gameDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        applicationOutPort.save(Application.create("1", 1L, "10", gameDate));

        assertThatThrownBy(() -> applicationService.apply(1L, "20"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.ALREADY_APPLIED_ON_DATE);
    }

    @Test
    @DisplayName("같은 날짜 신청을 취소한 뒤에는 같은 날짜 다른 경기에 신청할 수 있다")
    void apply_afterCancelOnSameDate_succeeds() {
        LocalDate gameDate = LocalDate.now().plusDays(1);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", gameDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        gameOutPort.games.put("20", new Game(
                "20", "KIA", "KT", "수원", gameDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        var first = applicationService.apply(1L, "10");
        applicationService.cancel(1L, first.id());

        var result = applicationService.apply(1L, "20");

        assertThat(result.status()).isEqualTo("waiting");
        assertThat(result.game().id()).isEqualTo("20");
    }

    @Test
    @DisplayName("다른 날짜 경기는 기존 신청이 있어도 신청할 수 있다")
    void apply_withApplicationOnDifferentDate_succeeds() {
        LocalDate firstDate = LocalDate.now().plusDays(1);
        LocalDate secondDate = LocalDate.now().plusDays(2);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", firstDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        gameOutPort.games.put("20", new Game(
                "20", "KIA", "KT", "수원", secondDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        applicationService.apply(1L, "10");
        var result = applicationService.apply(1L, "20");

        assertThat(result.status()).isEqualTo("waiting");
        assertThat(result.game().id()).isEqualTo("20");
    }

    @Test
    @DisplayName("매칭중·채팅중 신청도 같은 날짜 다른 경기 신청을 막는다")
    void apply_withMatchedOrChattingApplicationOnSameDate_throwsAlreadyAppliedOnDate() {
        LocalDate gameDate = LocalDate.now().plusDays(1);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", gameDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        gameOutPort.games.put("20", new Game(
                "20", "KIA", "KT", "수원", gameDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        gameOutPort.games.put("30", new Game(
                "30", "SSG", "NC", "문학", gameDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        Application matched = Application.create("1", 1L, "10", gameDate);
        matched.assign(2L);
        Application chatting = Application.create("2", 3L, "20", gameDate);
        chatting.assign(4L);
        chatting.confirm("30");
        applicationOutPort.save(matched);
        applicationOutPort.save(chatting);

        assertThatThrownBy(() -> applicationService.apply(1L, "20"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.ALREADY_APPLIED_ON_DATE);
        assertThatThrownBy(() -> applicationService.apply(3L, "30"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.ALREADY_APPLIED_ON_DATE);
    }

    @Test
    @DisplayName("같은 경기 동시 신청으로 DB 게임 유니크 제약이 충돌하면 이미 신청한 경기 상태로 응답한다")
    void apply_whenGameUniqueConstraintFails_throwsAlreadyApplied() {
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        applicationOutPort.failOnSaveAndFlushGameRace = true;

        assertThatThrownBy(() -> applicationService.apply(1L, "10"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.ALREADY_APPLIED);
    }

    @Test
    @DisplayName("같은 날짜 다른 경기 동시 신청으로 DB 날짜 유니크 제약이 충돌하면 날짜 중복 상태로 응답한다")
    void apply_whenDateUniqueConstraintFails_throwsAlreadyAppliedOnDate() {
        LocalDate gameDate = LocalDate.now().plusDays(1);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", gameDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        gameOutPort.games.put("20", new Game(
                "20", "KIA", "KT", "수원", gameDate,
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        applicationOutPort.concurrentDateRaceGameId = "20";

        assertThatThrownBy(() -> applicationService.apply(1L, "10"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.ALREADY_APPLIED_ON_DATE);
    }

    @Test
    @DisplayName("매칭을 거절하면 상대 신청을 대기 상태로 돌리고 상대에게 알림을 보낸다")
    void reject_matchedApplication_resetsOpponentAndNotifies() {
        Application mine = Application.create("1", 1L, "10");
        Application opponent = Application.create("2", 2L, "10");
        mine.assign(2L);
        opponent.assign(1L);
        applicationOutPort.save(mine);
        applicationOutPort.save(opponent);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        var result = applicationService.reject(1L, "1");

        assertThat(result.status()).isEqualTo("waiting");
        Application resetOpponent = applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow();
        assertThat(resetOpponent.getStatus()).isEqualTo("waiting");
        assertThat(resetOpponent.getMatchedMemberId()).isNull();
        assertThat(notificationUseCase.pushed).containsExactly("2:매칭이 취소됐어요");
    }

    @Test
    @DisplayName("일괄 매칭을 실행하면 경기별 대기자를 신청 순서대로 둘씩 짝지어 매칭한다")
    void matchWaitingApplications_pairsWaitingMembersPerGameAndLeavesOddOneWaiting() {
        applicationOutPort.save(Application.create("1", 1L, "10"));
        applicationOutPort.save(Application.create("2", 2L, "10"));
        applicationOutPort.save(Application.create("3", 3L, "10"));
        applicationOutPort.save(Application.create("4", 4L, "20"));
        putFutureGames("10", "20");

        var result = applicationService.matchWaitingApplications();

        assertThat(result.gamesProcessed()).isEqualTo(2);
        assertThat(result.gamesFailed()).isZero();
        assertThat(result.gamesSkipped()).isZero();
        assertThat(result.pairsMatched()).isEqualTo(1);
        Application first = applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow();
        Application second = applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow();
        assertThat(first.getStatus()).isEqualTo("matched");
        assertThat(first.getMatchedMemberId()).isEqualTo(2L);
        assertThat(second.getStatus()).isEqualTo("matched");
        assertThat(second.getMatchedMemberId()).isEqualTo(1L);
        assertThat(applicationOutPort.findByIdAndMemberId("3", 3L).orElseThrow().getStatus())
                .isEqualTo("waiting");
        assertThat(applicationOutPort.findByIdAndMemberId("4", 4L).orElseThrow().getStatus())
                .isEqualTo("waiting");
    }

    @Test
    @DisplayName("거절 이력이 있는 상대와는 다음 일괄 매칭에서 다시 매칭되지 않는다")
    void matchWaitingApplications_withRejectedOpponent_doesNotRematchSamePair() {
        Application mine = Application.create("1", 1L, "10");
        Application rejected = Application.create("2", 2L, "10");
        mine.assign(2L);
        mine.reject();
        applicationOutPort.save(mine);
        applicationOutPort.save(rejected);
        putFutureGames("10");

        var result = applicationService.matchWaitingApplications();

        assertThat(result.gamesFailed()).isZero();
        assertThat(result.pairsMatched()).isZero();
        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("waiting");
        assertThat(applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow().getStatus())
                .isEqualTo("waiting");
    }

    @Test
    @DisplayName("한 경기 매칭이 실패해도 다른 경기 매칭은 계속 처리한다")
    void matchWaitingApplications_whenOneGameFails_continuesWithOtherGames() {
        applicationOutPort.save(Application.create("1", 1L, "10"));
        applicationOutPort.save(Application.create("2", 2L, "10"));
        applicationOutPort.save(Application.create("3", 3L, "20"));
        applicationOutPort.save(Application.create("4", 4L, "20"));
        applicationOutPort.failOnFindWaitingGameIds.add("10");
        putFutureGames("10", "20");

        var result = applicationService.matchWaitingApplications();

        assertThat(result.gamesProcessed()).isEqualTo(2);
        assertThat(result.gamesFailed()).isEqualTo(1);
        assertThat(result.pairsMatched()).isEqualTo(1);
        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("waiting");
        assertThat(applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow().getStatus())
                .isEqualTo("waiting");
        assertThat(applicationOutPort.findByIdAndMemberId("3", 3L).orElseThrow().getStatus())
                .isEqualTo("matched");
        assertThat(applicationOutPort.findByIdAndMemberId("4", 4L).orElseThrow().getStatus())
                .isEqualTo("matched");
    }

    @Test
    @DisplayName("경기 매칭 중 예외가 발생해도 보정 조회로 실패 경기 대기 인원을 총 신청 수에 반영한다")
    void matchWaitingApplications_whenGameFails_countsApplicantsWithFallbackLookup() {
        applicationOutPort.save(Application.create("1", 1L, "10"));
        applicationOutPort.save(Application.create("2", 2L, "10"));
        applicationOutPort.save(Application.create("3", 3L, "20"));
        applicationOutPort.save(Application.create("4", 4L, "20"));
        applicationOutPort.failOnFindWaitingOnceGameIds.add("10");
        putFutureGames("10", "20");

        var result = applicationService.matchWaitingApplications();

        assertThat(result.gamesProcessed()).isEqualTo(2);
        assertThat(result.gamesFailed()).isEqualTo(1);
        assertThat(result.totalApplicants()).isEqualTo(4);
        assertThat(result.unmatchedPeople()).isEqualTo(2);
    }

    @Test
    @DisplayName("회원 조회에 실패한 대기 신청자는 제외하고 같은 경기의 나머지 신청자끼리 매칭한다")
    void matchWaitingApplications_withMissingMember_excludesApplicationAndMatchesRemainingCandidates() {
        applicationOutPort.save(Application.create("1", 1L, "10"));
        applicationOutPort.save(Application.create("2", 2L, "10"));
        applicationOutPort.save(Application.create("3", 3L, "10"));
        memberUseCase.missingMemberIds.add(2L);
        putFutureGames("10");

        var result = applicationService.matchWaitingApplications();

        assertThat(result.gamesProcessed()).isEqualTo(1);
        assertThat(result.gamesFailed()).isZero();
        assertThat(result.pairsMatched()).isEqualTo(1);
        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("matched");
        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getMatchedMemberId())
                .isEqualTo(3L);
        assertThat(applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow().getStatus())
                .isEqualTo("waiting");
        assertThat(applicationOutPort.findByIdAndMemberId("3", 3L).orElseThrow().getMatchedMemberId())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("한 경기의 유일한 대기자가 회원 조회 실패해도 전체 배치와 다른 경기는 죽지 않는다")
    void matchWaitingApplications_withOnlyMissingMemberInGame_doesNotFailBatch() {
        applicationOutPort.save(Application.create("1", 1L, "10"));
        applicationOutPort.save(Application.create("2", 2L, "20"));
        applicationOutPort.save(Application.create("3", 3L, "20"));
        memberUseCase.missingMemberIds.add(1L);
        putFutureGames("10", "20");

        var result = applicationService.matchWaitingApplications();

        assertThat(result.gamesProcessed()).isEqualTo(2);
        assertThat(result.gamesFailed()).isZero();
        assertThat(result.pairsMatched()).isEqualTo(1);
        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("waiting");
        assertThat(applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow().getStatus())
                .isEqualTo("matched");
        assertThat(applicationOutPort.findByIdAndMemberId("3", 3L).orElseThrow().getStatus())
                .isEqualTo("matched");
    }

    @Test
    @DisplayName("경기 시작 시각이 지난 게임의 대기 신청은 매칭 시도 없이 취소하고 알림을 보낸다")
    void matchWaitingApplications_withStartedGame_cancelsWaitingApplicationsAndSkipsMatching() {
        applicationOutPort.save(Application.create("1", 1L, "10"));
        applicationOutPort.save(Application.create("2", 2L, "10"));
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().minusDays(1),
                java.time.LocalTime.NOON, LocalDate.now().minusDays(2), null, null, null, null, null, null));

        var result = applicationService.matchWaitingApplications();

        assertThat(result.gamesProcessed()).isEqualTo(1);
        assertThat(result.gamesFailed()).isZero();
        assertThat(result.gamesSkipped()).isEqualTo(1);
        assertThat(result.pairsMatched()).isZero();
        assertThat(result.totalApplicants()).isEqualTo(2);
        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("cancelled");
        assertThat(applicationOutPort.findByIdAndMemberId("2", 2L).orElseThrow().getStatus())
                .isEqualTo("cancelled");
        assertThat(notificationUseCase.pushed).containsExactly("1:매칭이 취소됐어요", "2:매칭이 취소됐어요");
    }

    @Test
    @DisplayName("게임을 찾을 수 없는 대기 신청도 스킵 처리하고 취소한다")
    void matchWaitingApplications_withMissingGame_cancelsWaitingApplicationsAndSkipsMatching() {
        applicationOutPort.save(Application.create("1", 1L, "10"));

        var result = applicationService.matchWaitingApplications();

        assertThat(result.gamesProcessed()).isEqualTo(1);
        assertThat(result.gamesSkipped()).isEqualTo(1);
        assertThat(applicationOutPort.findByIdAndMemberId("1", 1L).orElseThrow().getStatus())
                .isEqualTo("cancelled");
    }

    @Test
    @DisplayName("단일 경기 매칭은 실제 경기 리그 ID를 사용하고 없는 경기는 실패한다")
    void matchWaitingPairs_usesGameLeagueIdAndMissingGameThrows() {
        applicationOutPort.save(Application.create("1", 1L, "10"));
        applicationOutPort.save(Application.create("2", 2L, "10"));
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null, 2L));
        memberUseCase.leagueId = 2L;

        var result = matchingBatchProcessor.matchWaitingPairs("10");

        assertThat(result.pairsMatched()).isEqualTo(1);
        assertThatThrownBy(() -> matchingBatchProcessor.matchWaitingPairs("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    @DisplayName("대기 상태가 아닌 신청은 프로필 미리보기를 비워둔다")
    void get_matchedApplication_returnsEmptyPreview() {
        Application mine = Application.create("1", 1L, "10");
        Application opponent = Application.create("2", 2L, "10");
        mine.assign(2L);
        opponent.assign(1L);
        applicationOutPort.save(mine);
        applicationOutPort.save(opponent);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        var result = applicationService.get(1L, "1");

        assertThat(result.waitingPreview()).isEmpty();
    }

    @Test
    @DisplayName("상세 조회에서는 허용된 매칭 이유만 기여도 내림차순 상위 3개로 노출한다")
    void get_matchedApplication_returnsTopThreeExposedMatchReasons() {
        Application mine = Application.create("1", 1L, "10");
        mine.assign(2L, 88, List.of(
                new MatchReason("gender", 15.0),
                new MatchReason("team", 13.0),
                new MatchReason("distance", 10.0),
                new MatchReason("trust", 12.0),
                new MatchReason("watchStyle", 7.0),
                new MatchReason("smoking", 8.0)));
        applicationOutPort.save(mine);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        var result = applicationService.get(1L, "1");

        assertThat(result.matchedProfile().matchReasons()).containsExactly(
                new MemberProfile.MatchReasonSummary("team", "응원팀이 같아요"),
                new MemberProfile.MatchReasonSummary("distance", "가까이 살아요"),
                new MemberProfile.MatchReasonSummary("smoking", "흡연 선호가 맞아요"));
    }

    @Test
    @DisplayName("노출 가능한 매칭 이유가 없거나 과거 데이터면 상세 조회에서 빈 배열로 정규화한다")
    void get_matchedApplication_withoutExposedMatchReasons_returnsEmptyReasons() {
        Application sensitiveOnly = Application.create("1", 1L, "10");
        sensitiveOnly.assign(2L, 70, List.of(
                new MatchReason("gender", 15.0),
                new MatchReason("trust", 12.0),
                new MatchReason("age", 5.0)));
        applicationOutPort.save(sensitiveOnly);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        var result = applicationService.get(1L, "1");

        assertThat(result.matchedProfile().matchReasons()).isEmpty();
    }

    @Test
    @DisplayName("목록 조회에서는 매칭 이유를 포함하지 않는다")
    void applications_matchedApplication_omitsMatchReasons() {
        Application mine = Application.create("1", 1L, "10");
        mine.assign(2L, 88, List.of(new MatchReason("team", 13.0)));
        applicationOutPort.save(mine);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));

        var result = applicationService.applications(1L, null, null);

        assertThat(result.get(0).matchedProfile().matchReasons()).isNull();
    }

    @Test
    @DisplayName("상세 조회에서 reviewed 신청이면 내가 쓴 리뷰를 포함한다")
    void get_reviewedApplication_includesMyReview() {
        Application mine = Application.create("1", 1L, "10");
        mine.assign(2L);
        mine.confirm("30");
        mine.completeGame();
        mine.review();
        applicationOutPort.save(mine);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().minusDays(1),
                java.time.LocalTime.NOON, LocalDate.now().minusDays(2), null, null, null, null, null, null));
        reviewOutPort.reviews.put("30:1", new ReviewDetail(
                5, List.of("시간 약속을 잘 지켜요"), "좋았어요", true, List.of(),
                LocalDateTime.of(2026, 7, 15, 21, 0)));

        var result = applicationService.get(1L, "1");

        assertThat(result.myReview()).isNotNull();
        assertThat(result.myReview().rating()).isEqualTo(5);
        assertThat(result.myReview().tags()).containsExactly("시간 약속을 잘 지켜요");
    }

    @Test
    @DisplayName("목록 조회에서는 reviewed 신청이어도 내가 쓴 리뷰를 포함하지 않는다")
    void applications_reviewedApplication_omitsMyReview() {
        Application mine = Application.create("1", 1L, "10");
        mine.assign(2L);
        mine.confirm("30");
        mine.completeGame();
        mine.review();
        applicationOutPort.save(mine);
        gameOutPort.games.put("10", new Game(
                "10", "LG", "DOOSAN", "잠실", LocalDate.now().minusDays(1),
                java.time.LocalTime.NOON, LocalDate.now().minusDays(2), null, null, null, null, null, null));
        reviewOutPort.reviews.put("30:1", new ReviewDetail(
                5, List.of("시간 약속을 잘 지켜요"), "좋았어요", true, List.of(),
                LocalDateTime.of(2026, 7, 15, 21, 0)));

        var result = applicationService.applications(1L, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).myReview()).isNull();
    }

    private static class FakeApplicationOutPort implements ApplicationOutPort {
        private final Map<String, Application> applications = new LinkedHashMap<>();
        private final Set<String> failOnFindWaitingGameIds = new java.util.HashSet<>();
        private final Set<String> failOnFindWaitingOnceGameIds = new java.util.HashSet<>();
        private final Set<String> failOnSaveMatchedGameIds = new java.util.HashSet<>();
        private long chattingCancellationCount = 0;
        private long nextId = 100;
        private boolean failOnSaveAndFlushGameRace = false;
        private String concurrentDateRaceGameId = null;
        private boolean failOnSaveAndFlushOptimisticLock = false;

        @Override
        public Application save(Application application) {
            Application toStore = application.getId() != null ? application : Application.reconstitute(
                    String.valueOf(nextId++), application.getMemberId(), application.getGameId(), application.getGameDate(),
                    application.getStatus(), application.getAppliedAt(), application.getMatchedMemberId(),
                    application.getChatId(), application.getMatchedAt(), application.getExpiresAt(),
                    application.getResponse(), application.getCancelledAt(), application.getMatchScore(),
                    application.getMatchReasons(), application.getRejectedMemberIds(), application.getVersion());
            if (failOnSaveMatchedGameIds.contains(toStore.getGameId()) && "matched".equals(toStore.getStatus())) {
                throw new IllegalStateException("broken match save");
            }
            applications.put(toStore.getId(), toStore);
            return toStore;
        }

        @Override
        public Application saveAndFlush(Application application) {
            if (failOnSaveAndFlushGameRace) {
                saveConcurrentApplication(application.getMemberId(), application.getGameId(), application.getGameDate());
                throw new DataIntegrityViolationException("duplicate active application by game");
            }
            if (concurrentDateRaceGameId != null) {
                saveConcurrentApplication(application.getMemberId(), concurrentDateRaceGameId, application.getGameDate());
                throw new DataIntegrityViolationException("duplicate active application by date");
            }
            if (failOnSaveAndFlushOptimisticLock) {
                throw new ObjectOptimisticLockingFailureException(Application.class, application.getId());
            }
            return save(application);
        }

        private void saveConcurrentApplication(Long memberId, String gameId, LocalDate gameDate) {
            String id = String.valueOf(nextId++);
            applications.put(id, Application.create(id, memberId, gameId, gameDate));
        }

        private final List<String> createdMatches = new java.util.ArrayList<>();

        @Override
        public String createMatch(Application application, Application opponent) {
            createdMatches.add(application.getId() + ":" + opponent.getId());
            return "30";
        }

        @Override
        public String createSoloMatch(Application application) {
            createdMatches.add("solo:" + application.getId());
            return "30";
        }

        @Override
        public void addParticipant(String chatId, Application application) {
            createdMatches.add("join:" + chatId + ":" + application.getId());
        }

        @Override
        public Optional<Application> findByIdAndMemberId(String id, Long memberId) {
            return Optional.ofNullable(applications.get(id))
                    .filter(application -> memberId.equals(application.getMemberId()));
        }

        @Override
        public Optional<Application> findByMemberIdAndGameId(Long memberId, String gameId) {
            // 실제 쿼리(findFirst...StatusNotOrderByAppliedAtDesc)와 동일하게 cancelled를 제외하고 최신 1건만 반환한다.
            return applications.values().stream()
                    .filter(application -> memberId.equals(application.getMemberId()))
                    .filter(application -> gameId.equals(application.getGameId()))
                    .filter(application -> !"cancelled".equals(application.getStatus()))
                    .reduce((first, second) -> second);
        }

        @Override
        public Optional<Application> findByChatIdAndMemberId(String chatId, Long memberId) {
            return applications.values().stream()
                    .filter(application -> chatId.equals(application.getChatId()))
                    .filter(application -> memberId.equals(application.getMemberId()))
                    .findFirst();
        }

        @Override
        public List<Application> findByMemberId(Long memberId) {
            return applications.values().stream()
                    .filter(application -> memberId.equals(application.getMemberId()))
                    .toList();
        }

        @Override
        public List<Application> findWaitingByGameId(String gameId) {
            if (failOnFindWaitingOnceGameIds.remove(gameId)) {
                throw new IllegalStateException("broken game data once");
            }
            if (failOnFindWaitingGameIds.contains(gameId)) {
                throw new IllegalStateException("broken game data");
            }
            return applications.values().stream()
                    .filter(application -> gameId.equals(application.getGameId()))
                    .filter(application -> "waiting".equals(application.getStatus()))
                    .toList();
        }

        @Override
        public List<String> findGameIdsWithWaitingApplications() {
            return applications.values().stream()
                    .filter(application -> "waiting".equals(application.getStatus()))
                    .map(Application::getGameId)
                    .distinct()
                    .toList();
        }

        @Override
        public boolean existsActiveByMemberIdAndGameId(Long memberId, String gameId) {
            return applications.values().stream()
                    .filter(application -> memberId.equals(application.getMemberId()))
                    .filter(application -> gameId.equals(application.getGameId()))
                    .anyMatch(application -> !"cancelled".equals(application.getStatus()));
        }

        @Override
        public boolean existsActiveByMemberIdAndDate(Long memberId, LocalDate date) {
            return applications.values().stream()
                    .filter(application -> memberId.equals(application.getMemberId()))
                    .filter(application -> date.equals(application.getGameDate()))
                    .anyMatch(application -> !"cancelled".equals(application.getStatus()));
        }

        @Override
        public List<LocalDate> findAppliedDates(Long memberId, int year, int month) {
            return List.of();
        }

        @Override
        public long countChattingCancellationsSince(Long memberId, LocalDateTime since) {
            return chattingCancellationCount;
        }
    }

    private static class FakeReviewOutPort implements ReviewOutPort {
        private final Map<String, ReviewDetail> reviews = new LinkedHashMap<>();

        @Override
        public boolean existsByMatchIdAndReviewerId(String matchId, Long reviewerId) {
            return reviews.containsKey(key(matchId, reviewerId));
        }

        @Override
        public Optional<ReviewDetail> findByMatchIdAndReviewerId(String matchId, Long reviewerId) {
            return Optional.ofNullable(reviews.get(key(matchId, reviewerId)));
        }

        @Override
        public void save(String matchId, Long reviewerId, Long targetMemberId, int rating,
                List<String> tags, String comment, Boolean profileAccurate,
                List<String> profileMismatchFields) {
            reviews.put(key(matchId, reviewerId), new ReviewDetail(
                    rating, tags, comment, profileAccurate, profileMismatchFields, LocalDateTime.now()));
        }

        private String key(String matchId, Long reviewerId) {
            return matchId + ":" + reviewerId;
        }
    }

    private static class FakeGameOutPort implements GameOutPort {
        private final Map<String, Game> games = new LinkedHashMap<>();

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
            return List.of();
        }

        @Override
        public Optional<Game> findById(String id) {
            return Optional.ofNullable(games.get(id));
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
            return new UpsertResult(0, 0, List.of());
        }

        @Override
        public int cancelMissingSyncedGames(java.time.YearMonth month, List<String> fetchedKboGameIds) {
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

    private static class FakeMemberUseCase implements MemberUseCase {
        private final Map<Long, Integer> trustScores = new LinkedHashMap<>();
        private final Set<Long> missingMemberIds = new java.util.HashSet<>();
        private Long leagueId = 1L;

        @Override
        public MemberProfile get(Long memberId) {
            if (missingMemberIds.contains(memberId)) {
                throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
            }
            return new MemberProfile(
                    memberId, "0100000000" + memberId, "phone", "member" + memberId,
                    null, LocalDate.of(1997, 1, 1), "male", null, null, "LG", 0, 0.0,
                    trustScores.getOrDefault(memberId, 100),
                    List.of(WatchStyle.CHEER), Personality.TENSION, TalkStyle.TALKATIVE,
                    SmokingStatus.NON_SMOKER, GenderPref.ANY, AgePref.ANY, SmokingPref.ANY, 5, false,
                    null, null, null, 0, 0, null).withLeagueProfile(new MemberLeagueProfile(
                            leagueId, null, 1L, null, null, List.of(WatchStyle.CHEER), List.of()));
        }

        @Override
        public MemberProfile updateProfile(Long memberId, String nickname, String bio,
                LocalDate birthdate, Gender gender, String team) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MemberProfile updateStyle(Long memberId, String team, List<WatchStyle> watchStyles,
                Personality personality, TalkStyle talkStyle, SmokingStatus smokingStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MemberProfile updatePreference(Long memberId, GenderPref genderPref, AgePref agePref,
                SmokingPref smokingPref, Integer distanceKm) {
            throw new UnsupportedOperationException();
        }

        @Override
        public LocationVerifyResult verifyLocation(Long memberId, double latitude, double longitude) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MemberProfile updateAvatar(Long memberId, String avatarUrl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TrustScoreResult getTrustScore(Long memberId) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeMemberOutPort implements MemberOutPort {
        private final Map<Long, Member> members = new LinkedHashMap<>();
        private final Set<String> missingLeagueProfiles = new java.util.HashSet<>();

        @Override public Member save(Member member) {
            members.put(member.getId(), member);
            return member;
        }
        @Override public Optional<Member> findById(Long id) { return Optional.ofNullable(members.get(id)); }
        @Override public Optional<Member> findByPhone(String phone) { return Optional.empty(); }
        @Override public Optional<Member> findByProvider(com.sportsmate.server.domain.member.enums.LoginType loginType,
                String providerId) { return Optional.empty(); }
        @Override public Optional<String> findExpoPushTokenById(Long id) { return Optional.empty(); }
        @Override public boolean isWelcomeNotified(Long id) { return false; }
        @Override public boolean existsByPhone(String phone) { return false; }
        @Override public boolean existsByNickname(String nickname) { return false; }
        @Override public void updateExpoPushToken(Long id, String expoPushToken) {}
        @Override public boolean markWelcomeNotified(Long id) { return false; }
        @Override public void withdraw(Long id) {}
        @Override public boolean existsLeagueProfile(Long memberId, Long leagueId) {
            return !missingLeagueProfiles.contains(memberId + ":" + leagueId);
        }
    }

    private static Member member(Long id, int trustScore) {
        return Member.reconstitute(
                id, "0100000000" + id, "encoded", LoginType.PHONE, null,
                "member" + id, "소개", LocalDate.of(1997, 1, 1), Gender.MALE, null,
                "#2E7D32", "LG", List.of(WatchStyle.CHEER), Personality.TENSION,
                TalkStyle.TALKATIVE, SmokingStatus.NON_SMOKER, GenderPref.ANY, AgePref.ANY,
                SmokingPref.ANY, 5, false, null, null, null, 0, 0.0, trustScore,
                0, 0, false, Role.USER);
    }

    private void putFutureGames(String... gameIds) {
        for (String gameId : gameIds) {
            gameOutPort.games.put(gameId, new Game(
                    gameId, "LG", "DOOSAN", "잠실", LocalDate.now().plusDays(1),
                    java.time.LocalTime.NOON, LocalDate.now(), null, null, null, null, null, null));
        }
    }

    private static class FakeChatUseCase implements ChatUseCase {
        private final List<String> systemMessages = new java.util.ArrayList<>();

        @Override public ChatRoomResult room(Long memberId, String chatId) {
            throw new UnsupportedOperationException();
        }
        @Override public MessagesResult messages(Long memberId, String chatId, String cursor, int size) {
            throw new UnsupportedOperationException();
        }
        @Override public MessageResult send(Long memberId, String chatId, String text) {
            throw new UnsupportedOperationException();
        }
        @Override public MessageResult postSystemMessage(String chatId, String text) {
            systemMessages.add(chatId + ":" + text);
            return null;
        }
        @Override public boolean isParticipant(Long memberId, String chatId) {
            return false;
        }
        @Override public boolean setNotificationEnabled(Long memberId, String chatId, boolean enabled) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeNotificationUseCase implements NotificationUseCase {
        @Override
        public NotificationsResult notifications(Long memberId, String cursor, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long unreadCount(Long memberId) {
            return 0;
        }

        @Override
        public void read(Long memberId, String notificationId) {
        }

        @Override
        public void readAll(Long memberId) {
        }

        @Override
        public SettingsResult settings(Long memberId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SettingsResult updateSettings(Long memberId, SettingsCommand command) {
            throw new UnsupportedOperationException();
        }

        private final List<String> pushed = new java.util.ArrayList<>();

        @Override
        public void createAndPush(Long memberId, String type, String title, String body, String targetKind,
                String applicationId, String chatId) {
            pushed.add(memberId + ":" + title);
        }
    }
}
