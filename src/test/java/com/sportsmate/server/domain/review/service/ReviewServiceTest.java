package com.sportsmate.server.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.game.Game;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.Gender;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.LoginType;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.notification.port.in.NotificationUseCase;
import com.sportsmate.server.domain.review.exception.ReviewErrorCode;
import com.sportsmate.server.domain.review.port.dto.ReviewDetail;
import com.sportsmate.server.domain.review.port.out.ReviewOutPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("ReviewService 단위 테스트")
class ReviewServiceTest {

    private final FakeReviewOutPort reviewOutPort = new FakeReviewOutPort();
    private final FakeApplicationOutPort applicationOutPort = new FakeApplicationOutPort();
    private final FakeGameOutPort gameOutPort = new FakeGameOutPort();
    private final FakeMemberOutPort memberOutPort = new FakeMemberOutPort();
    private final FakeNotificationUseCase notificationUseCase = new FakeNotificationUseCase();
    private final AuditLogPort auditLogPort = Mockito.mock(AuditLogPort.class);
    private final ReviewService reviewService = new ReviewService(
            reviewOutPort, applicationOutPort, gameOutPort, memberOutPort, notificationUseCase, auditLogPort);

    @Test
    @DisplayName("리뷰 작성 후 상대가 아직 리뷰하지 않았다면 리뷰 요청 알림을 보낸다")
    void review_whenOpponentNotReviewed_sendsReviewRequestNotification() {
        givenReviewableMatch();

        reviewService.review(1L, "10", 5, List.of("또 만나고 싶어요"), "좋았어요", null, List.of());

        assertThat(notificationUseCase.notifications).hasSize(1);
        FakeNotificationUseCase.Notification notification = notificationUseCase.notifications.get(0);
        assertThat(notification.memberId()).isEqualTo(2L);
        assertThat(notification.type()).isEqualTo("review");
        assertThat(notification.targetKind()).isEqualTo("review");
        assertThat(notification.applicationId()).isEqualTo("20");
        assertThat(notification.title()).isEqualTo("동행 후기를 남겨주세요");
        assertThat(notification.body()).isEqualTo("리뷰어님과의 직관은 어떠셨나요?");
        verify(auditLogPort).record(argThat(event ->
                event.category() == AuditCategory.TRUST_SCORE
                        && event.action().equals("TRUST_SCORE_ADD")
                        && event.targetId().equals("1")
                        && event.detail().get("delta").equals(3)));
        verify(auditLogPort).record(argThat(event ->
                event.category() == AuditCategory.TRUST_SCORE
                        && event.action().equals("TRUST_SCORE_ADD")
                        && event.targetId().equals("2")
                        && event.detail().get("delta").equals(5)));
    }

    @Test
    @DisplayName("상대가 이미 리뷰했다면 리뷰 요청 알림을 보내지 않는다")
    void review_whenOpponentAlreadyReviewed_doesNotSendReviewRequestNotification() {
        givenReviewableMatch();
        reviewOutPort.reviewedMatchIdsByReviewerId.put(2L, "30");

        reviewService.review(1L, "10", 5, List.of("또 만나고 싶어요"), "좋았어요", null, List.of());

        assertThat(notificationUseCase.notifications).isEmpty();
    }

    @Test
    @DisplayName("받은 별점에 따라 대상자 신뢰도 델타를 반영한다")
    void review_withRatingDelta_updatesTargetTrustScore() {
        givenReviewableMatch();
        memberOutPort.members.put(2L, memberWithTrustScore(2L, "상대", 4));

        reviewService.review(1L, "10", 2, List.of("시간 약속을 안 지켰어요"),
                "늦으셨어요", false, List.of("smoking", "watch_style"));

        assertThat(memberOutPort.members.get(2L).getTrustScore()).isEqualTo(1);
        verify(auditLogPort).record(argThat(event ->
                event.category() == AuditCategory.TRUST_SCORE
                        && event.action().equals("TRUST_SCORE_DEDUCT")
                        && event.targetId().equals("2")
                        && event.detail().get("delta").equals(-3)
                        && event.detail().get("reason").equals("LOW_RATING_RECEIVED")));
        assertThat(reviewOutPort.savedProfileAccurate).isFalse();
        assertThat(reviewOutPort.savedProfileMismatchFields).containsExactly("smoking", "watch_style");
    }

    @Test
    @DisplayName("별점 구간과 맞지 않는 태그는 거절한다")
    void review_withMismatchedTag_throwsInvalidReviewTag() {
        givenReviewableMatch();

        assertThatThrownBy(() -> reviewService.review(1L, "10", 5,
                List.of("시간 약속을 안 지켰어요"), "아쉬웠어요", null, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.INVALID_REVIEW_TAG);
    }

    @Test
    @DisplayName("프로필이 일치하거나 미응답이면 불일치 항목을 저장하지 않는다")
    void review_withProfileAccurateTrue_ignoresMismatchFields() {
        givenReviewableMatch();

        reviewService.review(1L, "10", 4, List.of("대화가 잘 통해요"),
                "좋았어요", true, List.of("smoking"));

        assertThat(reviewOutPort.savedProfileAccurate).isTrue();
        assertThat(reviewOutPort.savedProfileMismatchFields).isEmpty();
    }

    private void givenReviewableMatch() {
        LocalDateTime now = LocalDateTime.now();
        applicationOutPort.save(Application.reconstitute(
                "10", 1L, "100", "game_done", now.minusDays(1),
                2L, "30", now.minusDays(1), now.plusHours(1), "accepted", null, 90, Set.of()));
        applicationOutPort.save(Application.reconstitute(
                "20", 2L, "100", "game_done", now.minusDays(1),
                1L, "30", now.minusDays(1), now.plusHours(1), "accepted", null, 90, Set.of()));
        gameOutPort.games.put("100", new Game(
                "100", "LG", "DOOSAN", "잠실", LocalDate.now().minusDays(1),
                LocalTime.NOON, LocalDate.now().minusDays(2), null, null, null, null, null, null));
        memberOutPort.members.put(1L, member(1L, "리뷰어"));
        memberOutPort.members.put(2L, member(2L, "상대"));
    }

    private Member member(Long id, String nickname) {
        return memberWithTrustScore(id, nickname, 100);
    }

    private Member memberWithTrustScore(Long id, String nickname, int trustScore) {
        return Member.reconstitute(
                id,
                "0100000000" + id,
                "password",
                LoginType.PHONE,
                null,
                nickname,
                "소개",
                LocalDate.of(1997, 1, 1),
                Gender.MALE,
                null,
                "#2E7D32",
                "LG",
                List.of(WatchStyle.CHEER),
                Personality.TENSION,
                TalkStyle.TALKATIVE,
                SmokingStatus.NON_SMOKER,
                GenderPref.ANY,
                AgePref.ANY,
                SmokingPref.ANY,
                5,
                true,
                "서울 송파구 잠실동",
                37.5,
                127.0,
                0,
                0.0,
                trustScore,
                0,
                0,
                false,
                Role.USER);
    }

    private static class FakeReviewOutPort implements ReviewOutPort {
        private final Map<Long, String> reviewedMatchIdsByReviewerId = new LinkedHashMap<>();
        private Boolean savedProfileAccurate;
        private List<String> savedProfileMismatchFields = List.of();

        @Override
        public boolean existsByMatchIdAndReviewerId(String matchId, Long reviewerId) {
            return matchId.equals(reviewedMatchIdsByReviewerId.get(reviewerId));
        }

        @Override
        public Optional<ReviewDetail> findByMatchIdAndReviewerId(String matchId, Long reviewerId) {
            return Optional.empty();
        }

        @Override
        public void save(String matchId, Long reviewerId, Long targetMemberId, int rating,
                List<String> tags, String comment, Boolean profileAccurate,
                List<String> profileMismatchFields) {
            reviewedMatchIdsByReviewerId.put(reviewerId, matchId);
            savedProfileAccurate = profileAccurate;
            savedProfileMismatchFields = profileMismatchFields;
        }
    }

    private static class FakeApplicationOutPort implements ApplicationOutPort {
        private final Map<String, Application> applications = new LinkedHashMap<>();

        @Override
        public Application save(Application application) {
            applications.put(application.getId(), application);
            return application;
        }

        @Override
        public String createMatch(Application application, Application opponent) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String createSoloMatch(Application application) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addParticipant(String chatId, Application application) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Application> findByIdAndMemberId(String id, Long memberId) {
            return Optional.ofNullable(applications.get(id))
                    .filter(application -> memberId.equals(application.getMemberId()));
        }

        @Override
        public Optional<Application> findByMemberIdAndGameId(Long memberId, String gameId) {
            return applications.values().stream()
                    .filter(application -> memberId.equals(application.getMemberId()))
                    .filter(application -> gameId.equals(application.getGameId()))
                    .findFirst();
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

    private static class FakeMemberOutPort implements MemberOutPort {
        private final Map<Long, Member> members = new LinkedHashMap<>();

        @Override
        public Member save(Member member) {
            members.put(member.getId(), member);
            return member;
        }

        @Override
        public Optional<Member> findById(Long id) {
            return Optional.ofNullable(members.get(id));
        }

        @Override
        public Optional<Member> findByPhone(String phone) {
            return Optional.empty();
        }

        @Override
        public Optional<Member> findByProvider(LoginType loginType, String providerId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findExpoPushTokenById(Long id) {
            return Optional.empty();
        }

        @Override
        public boolean isWelcomeNotified(Long id) {
            return false;
        }

        @Override
        public boolean existsByPhone(String phone) {
            return false;
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return false;
        }

        @Override
        public void updateExpoPushToken(Long id, String expoPushToken) {
        }

        @Override
        public boolean markWelcomeNotified(Long id) {
            return false;
        }

        @Override
        public void withdraw(Long id) {
        }
    }

    private static class FakeNotificationUseCase implements NotificationUseCase {
        private final List<Notification> notifications = new ArrayList<>();

        @Override
        public NotificationsResult notifications(Long memberId, String cursor, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long unreadCount(Long memberId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void read(Long memberId, String notificationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void readAll(Long memberId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SettingsResult settings(Long memberId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SettingsResult updateSettings(Long memberId, SettingsCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void createAndPush(Long memberId, String type, String title, String body,
                String targetKind, String applicationId, String chatId) {
            notifications.add(new Notification(
                    memberId, type, title, body, targetKind, applicationId, chatId));
        }

        private record Notification(Long memberId, String type, String title, String body,
                String targetKind, String applicationId, String chatId) {
        }
    }
}
