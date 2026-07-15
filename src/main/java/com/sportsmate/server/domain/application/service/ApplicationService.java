package com.sportsmate.server.domain.application.service;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.exception.ApplicationErrorCode;
import com.sportsmate.server.domain.application.policy.CancellationPenaltyPolicy;
import com.sportsmate.server.domain.application.port.in.ApplicationUseCase;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.chat.port.in.ChatUseCase;
import com.sportsmate.server.domain.game.Game;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.domain.game.service.GameService;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import com.sportsmate.server.domain.member.port.in.MemberUseCase;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.notification.port.in.NotificationUseCase;
import com.sportsmate.server.domain.review.port.dto.ReviewDetail;
import com.sportsmate.server.domain.review.port.out.ReviewOutPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationService implements ApplicationUseCase {
    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final Set<String> ACTIVE_STATUSES = Set.of("waiting", "matched", "chatting");
    private static final int MIN_TRUST_SCORE_TO_APPLY = 50;

    private final ApplicationOutPort applicationOutPort;
    private final GameOutPort gameOutPort;
    private final GameService gameService;
    private final MemberUseCase memberUseCase;
    private final MemberOutPort memberOutPort;
    private final NotificationUseCase notificationUseCase;
    private final ChatUseCase chatUseCase;
    private final ReviewOutPort reviewOutPort;
    private final ApplicationMatchingBatchProcessor matchingBatchProcessor;
    private final CancellationPenaltyPolicy cancellationPenaltyPolicy = new CancellationPenaltyPolicy();

    public ApplicationService(ApplicationOutPort applicationOutPort, GameOutPort gameOutPort,
            GameService gameService, MemberUseCase memberUseCase,
            MemberOutPort memberOutPort, NotificationUseCase notificationUseCase,
            ChatUseCase chatUseCase, ReviewOutPort reviewOutPort,
            ApplicationMatchingBatchProcessor matchingBatchProcessor) {
        this.applicationOutPort = applicationOutPort;
        this.gameOutPort = gameOutPort;
        this.gameService = gameService;
        this.memberUseCase = memberUseCase;
        this.memberOutPort = memberOutPort;
        this.notificationUseCase = notificationUseCase;
        this.chatUseCase = chatUseCase;
        this.reviewOutPort = reviewOutPort;
        this.matchingBatchProcessor = matchingBatchProcessor;
    }

    @Override
    @Transactional
    public ApplicationResult apply(Long memberId, String gameId) {
        Game game = findGame(gameId);
        if (!"open".equals(game.status(false, LocalDate.now()))) {
            throw new BusinessException(ApplicationErrorCode.APPLICATION_CLOSED);
        }
        if (memberUseCase.get(memberId).trustScore() < MIN_TRUST_SCORE_TO_APPLY) {
            throw new BusinessException(ApplicationErrorCode.TRUST_SCORE_TOO_LOW);
        }
        if (applicationOutPort.existsActiveByMemberIdAndGameId(memberId, gameId)) {
            throw new BusinessException(ApplicationErrorCode.ALREADY_APPLIED);
        }
        Application application = Application.create(null, memberId, gameId);
        Application saved = applicationOutPort.save(application);
        return get(memberId, saved.getId());
    }

    @Override
    public List<ApplicationResult> applications(Long memberId, LocalDate date, List<String> statuses) {
        return applicationOutPort.findByMemberId(memberId).stream()
                .map(application -> toResult(memberId, application, false))
                .filter(result -> date == null || result.game().date().equals(date))
                .filter(result -> statuses == null || statuses.isEmpty() || statuses.contains(result.status()))
                .toList();
    }

    @Override
    public List<LocalDate> calendar(Long memberId, int year, int month) {
        return applicationOutPort.findAppliedDates(memberId, year, month);
    }

    @Override
    public ApplicationResult get(Long memberId, String applicationId) {
        return toResult(memberId, find(memberId, applicationId), true);
    }

    @Override
    @Transactional
    public void cancel(Long memberId, String applicationId) {
        Application application = find(memberId, applicationId);
        boolean wasMatched = "matched".equals(application.getStatus());
        boolean wasChatting = "chatting".equals(application.getStatus());
        Long opponentId = application.getMatchedMemberId();
        String gameId = application.getGameId();
        String chatId = application.getChatId();
        try {
            application.cancel();
        } catch (IllegalStateException exception) {
            throw new BusinessException(ApplicationErrorCode.CANNOT_CANCEL);
        }
        applicationOutPort.save(application);
        if (wasChatting && opponentId != null && chatId != null) {
            cancelChattingOpponent(memberId, opponentId, gameId, chatId);
            applyCancellationPenalty(memberId);
        } else if (wasMatched && opponentId != null) {
            applicationOutPort.findByMemberIdAndGameId(opponentId, gameId)
                    .filter(opponent -> memberId.equals(opponent.getMatchedMemberId()))
                    .ifPresent(opponent -> {
                        opponent.resetToWaiting();
                        applicationOutPort.save(opponent);
                    });
            applyMatchedCancellationPenalty(memberId);
        }
    }

    @Override
    public MatchStatusResult status(Long memberId, String applicationId) {
        Application application = find(memberId, applicationId);
        return new MatchStatusResult(
                application.getStatus(),
                matchedProfile(application),
                application.getChatId(),
                application.getMatchedAt(),
                application.getExpiresAt());
    }

    @Override
    @Transactional
    public ApplicationResult accept(Long memberId, String applicationId) {
        Application application = find(memberId, applicationId);
        if (!"matched".equals(application.getStatus())) {
            throw new BusinessException(ApplicationErrorCode.MATCH_NOT_READY);
        }
        application.markAccepted();
        Application opponent = applicationOutPort
                .findByMemberIdAndGameId(application.getMatchedMemberId(), application.getGameId())
                .orElseThrow(() -> new BusinessException(ApplicationErrorCode.MATCH_NOT_READY));
        String chatId = opponent.getChatId();
        if (chatId == null) {
            chatId = applicationOutPort.createSoloMatch(application);
            notificationUseCase.createAndPush(opponent.getMemberId(), "match", "동행자가 채팅을 시작했어요",
                    "채팅방에 입장해 일정을 조율해보세요.", "chat", null, chatId);
        } else {
            applicationOutPort.addParticipant(chatId, application);
        }
        application.confirm(chatId);
        applicationOutPort.save(application);
        chatUseCase.postSystemMessage(chatId, memberUseCase.get(memberId).nickname() + "님이 채팅에 참여했어요.");
        return toResult(memberId, application, false);
    }

    @Override
    @Transactional
    public ApplicationResult reject(Long memberId, String applicationId) {
        Application application = find(memberId, applicationId);
        if (!"matched".equals(application.getStatus())) {
            throw new BusinessException(ApplicationErrorCode.MATCH_NOT_READY);
        }
        Long opponentId = application.getMatchedMemberId();
        Application opponent = applicationOutPort.findByMemberIdAndGameId(opponentId, application.getGameId())
                .orElseThrow(() -> new BusinessException(ApplicationErrorCode.MATCH_NOT_READY));
        if (opponent.getChatId() != null) {
            throw new BusinessException(ApplicationErrorCode.CANNOT_REJECT_AFTER_CHAT_STARTED);
        }
        application.reject();
        opponent.reject();
        applicationOutPort.save(opponent);
        applicationOutPort.save(application);
        return toResult(memberId, application, false);
    }

    @Override
    @Transactional
    public void cancelAllActiveByMember(Long memberId) {
        applicationOutPort.findByMemberId(memberId).stream()
                .filter(application -> ACTIVE_STATUSES.contains(application.getStatus()))
                .forEach(this::cancelForWithdrawal);
    }

    private void cancelForWithdrawal(Application application) {
        boolean wasChatting = "chatting".equals(application.getStatus());
        boolean wasMatched = "matched".equals(application.getStatus());
        Long opponentId = application.getMatchedMemberId();
        String gameId = application.getGameId();
        String chatId = application.getChatId();
        application.cancel();
        applicationOutPort.save(application);
        if (wasChatting && opponentId != null && chatId != null) {
            rematchOpponentAfterWithdrawal(opponentId, gameId, chatId);
        } else if (wasMatched && opponentId != null) {
            rematchOpponentAfterWithdrawal(opponentId, gameId, null);
        }
    }

    private void rematchOpponentAfterWithdrawal(Long opponentId, String gameId, String chatId) {
        applicationOutPort.findByMemberIdAndGameId(opponentId, gameId)
                .filter(opponent -> opponentId.equals(opponent.getMemberId()))
                .filter(opponent -> chatId == null || chatId.equals(opponent.getChatId()))
                .ifPresent(opponent -> {
                    opponent.resetToWaiting();
                    applicationOutPort.save(opponent);
                });
        if (chatId != null) {
            chatUseCase.postSystemMessage(chatId, "상대방이 서비스를 탈퇴해 채팅방이 종료됩니다.");
        }
        notificationUseCase.createAndPush(opponentId, "match", "매칭이 취소됐어요",
                "상대방이 서비스를 탈퇴해 매칭이 취소됐어요. 다시 매칭을 진행해드릴게요.", "findMatch", null, null);
    }

    @Override
    public MatchBatchResult matchWaitingApplications() {
        long startedAt = System.nanoTime();
        List<String> gameIds = applicationOutPort.findGameIdsWithWaitingApplications();
        int pairsMatched = 0;
        int gamesFailed = 0;
        int totalApplicants = 0;
        int personErrors = 0;
        int carryOver = 0;
        for (String gameId : gameIds) {
            try {
                var gameResult = matchingBatchProcessor.matchWaitingPairs(gameId);
                totalApplicants += gameResult.totalApplicants();
                pairsMatched += gameResult.pairsMatched();
                personErrors += gameResult.personErrors();
                carryOver += gameResult.carryOver();
            } catch (RuntimeException exception) {
                gamesFailed++;
                totalApplicants += waitingCountOrZero(gameId);
                log.error("Matching failed for game. gameId={}, reason={}", gameId, exception.getMessage(), exception);
            }
        }
        long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        int matchedPeople = pairsMatched * 2;
        int unmatchedPeople = Math.max(0, totalApplicants - matchedPeople - personErrors);
        return new MatchBatchResult(
                gameIds.size(),
                gamesFailed,
                pairsMatched,
                totalApplicants,
                unmatchedPeople,
                personErrors,
                carryOver,
                durationMs);
    }

    private int waitingCountOrZero(String gameId) {
        try {
            return applicationOutPort.findWaitingByGameId(gameId).size();
        } catch (RuntimeException exception) {
            log.warn("Failed to count waiting applications for failed matching game. gameId={}, reason={}",
                    gameId, exception.getMessage(), exception);
            return 0;
        }
    }

    private ApplicationResult toResult(Long memberId, Application application, boolean includeMyReview) {
        Game game = findGame(application.getGameId());
        return new ApplicationResult(
                application.getId(), gameService.toResult(memberId, game),
                application.getAppliedAt(), application.getStatus(),
                matchedProfile(application), application.getChatId(),
                application.getMatchedAt(), application.getExpiresAt(),
                waitingPreview(application),
                myReview(memberId, application, includeMyReview));
    }

    private ReviewDetail myReview(Long memberId, Application application, boolean includeMyReview) {
        if (!includeMyReview || !"reviewed".equals(application.getStatus()) || application.getChatId() == null) {
            return null;
        }
        return reviewOutPort.findByMatchIdAndReviewerId(application.getChatId(), memberId)
                .orElse(null);
    }

    private MemberProfile profile(Long memberId) {
        return memberId == null ? null : memberUseCase.get(memberId);
    }

    private MemberProfile matchedProfile(Application application) {
        MemberProfile profile = profile(application.getMatchedMemberId());
        return profile == null ? null : profile.withMatchScore(application.getMatchScore());
    }

    private List<WaitingParticipantPreview> waitingPreview(Application application) {
        if (!"waiting".equals(application.getStatus())) return List.of();
        return applicationOutPort.findWaitingByGameId(application.getGameId()).stream()
                .filter(other -> !other.getMemberId().equals(application.getMemberId()))
                .map(other -> memberUseCase.get(other.getMemberId()))
                .map(profile -> new WaitingParticipantPreview(
                        profile.nickname(), profile.avatarUrl(), profile.avatarColor()))
                .toList();
    }

    private void cancelChattingOpponent(Long memberId, Long opponentId, String gameId, String chatId) {
        applicationOutPort.findByMemberIdAndGameId(opponentId, gameId)
                .filter(opponent -> chatId.equals(opponent.getChatId()))
                .ifPresent(opponent -> {
                    opponent.cancel();
                    applicationOutPort.save(opponent);
                });
        String nickname = memberUseCase.get(memberId).nickname();
        chatUseCase.postSystemMessage(chatId, nickname + "님이 매칭을 취소했어요. 채팅방이 종료됩니다.");
        notificationUseCase.createAndPush(opponentId, "match", "매칭이 취소됐어요",
                nickname + "님이 매칭을 취소했어요.", "chat", null, chatId);
    }

    private void applyCancellationPenalty(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        long cancellationCount = applicationOutPort.countChattingCancellationsSince(
                memberId, cancellationPenaltyPolicy.since(now));
        int penaltyPoints = cancellationPenaltyPolicy.penaltyPoints(cancellationCount);
        if (penaltyPoints == 0) {
            return;
        }
        Member member = memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(com.sportsmate.server.domain.member.exception.MemberErrorCode.MEMBER_NOT_FOUND));
        member.addTrustScore(penaltyPoints);
        memberOutPort.save(member);
    }

    private void applyMatchedCancellationPenalty(Long memberId) {
        Member member = memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(com.sportsmate.server.domain.member.exception.MemberErrorCode.MEMBER_NOT_FOUND));
        member.addTrustScore(cancellationPenaltyPolicy.matchedCancelPenaltyPoints());
        memberOutPort.save(member);
    }

    private Application find(Long memberId, String applicationId) {
        return applicationOutPort.findByIdAndMemberId(applicationId, memberId)
                .orElseThrow(() -> new BusinessException(ApplicationErrorCode.APPLICATION_NOT_FOUND));
    }

    private Game findGame(String gameId) {
        return gameOutPort.findById(gameId)
                .orElseThrow(() -> new BusinessException(ApplicationErrorCode.GAME_NOT_FOUND));
    }
}
