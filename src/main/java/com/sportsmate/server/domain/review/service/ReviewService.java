package com.sportsmate.server.domain.review.service;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditEvent;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.common.port.out.audit.AuditResult;
import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.exception.ApplicationErrorCode;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.game.Game;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.exception.MemberErrorCode;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.notification.port.in.NotificationUseCase;
import com.sportsmate.server.domain.review.ReviewFeedbackPolicy;
import com.sportsmate.server.domain.review.exception.ReviewErrorCode;
import com.sportsmate.server.domain.review.port.in.ReviewUseCase;
import com.sportsmate.server.domain.review.port.out.ReviewOutPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService implements ReviewUseCase {
    private final ReviewOutPort reviewOutPort;
    private final ApplicationOutPort applicationOutPort;
    private final GameOutPort gameOutPort;
    private final MemberOutPort memberOutPort;
    private final NotificationUseCase notificationUseCase;
    private final AuditLogPort auditLogPort;

    public ReviewService(ReviewOutPort reviewOutPort, ApplicationOutPort applicationOutPort,
            GameOutPort gameOutPort, MemberOutPort memberOutPort,
            NotificationUseCase notificationUseCase, AuditLogPort auditLogPort) {
        this.reviewOutPort = reviewOutPort;
        this.applicationOutPort = applicationOutPort;
        this.gameOutPort = gameOutPort;
        this.memberOutPort = memberOutPort;
        this.notificationUseCase = notificationUseCase;
        this.auditLogPort = auditLogPort;
    }

    @Override
    @Transactional
    public ReviewResult review(Long memberId, String applicationId, int rating,
            List<String> tags, String comment, Boolean profileAccurate,
            List<String> profileMismatchFields) {
        List<String> validatedTags = ReviewFeedbackPolicy.validateTags(rating, tags);
        List<String> validatedProfileMismatchFields =
                ReviewFeedbackPolicy.validateProfileMismatchFields(profileAccurate, profileMismatchFields);

        Application application = applicationOutPort.findByIdAndMemberId(applicationId, memberId)
                .orElseThrow(() -> new BusinessException(ApplicationErrorCode.APPLICATION_NOT_FOUND));
        if (application.getChatId() != null
                && reviewOutPort.existsByMatchIdAndReviewerId(application.getChatId(), memberId)) {
            throw new BusinessException(ReviewErrorCode.ALREADY_REVIEWED);
        }
        Game game = gameOutPort.findById(application.getGameId())
                .orElseThrow(() -> new BusinessException(ApplicationErrorCode.GAME_NOT_FOUND));
        LocalDateTime gameAt = LocalDateTime.of(game.date(), game.time());
        if ("chatting".equals(application.getStatus()) && gameAt.isBefore(LocalDateTime.now())) {
            application.completeGame();
        }
        try {
            application.review();
        } catch (IllegalStateException exception) {
            throw new BusinessException(ReviewErrorCode.NOT_GAME_DONE);
        }
        Long targetId = application.getMatchedMemberId();
        reviewOutPort.save(application.getChatId(), memberId, targetId, rating,
                validatedTags, comment, profileAccurate, validatedProfileMismatchFields);
        applicationOutPort.save(application);

        Member reviewer = findMember(memberId);
        reviewer.addTrustScore(3);
        auditLogPort.record(AuditEvent.of(
                AuditCategory.TRUST_SCORE, "TRUST_SCORE_ADD", "SYSTEM", null,
                "MEMBER", memberId.toString(), AuditResult.SUCCESS,
                Map.of("delta", 3, "reason", "REVIEW_SUBMITTED", "applicationId", applicationId)));
        if (LocalDateTime.now().isBefore(gameAt.plusHours(48))) reviewer.addCoupon();
        memberOutPort.save(reviewer);

        Member target = findMember(targetId);
        target.receiveRating(rating);
        int targetDelta = ReviewFeedbackPolicy.trustScoreDeltaForRating(rating);
        if (targetDelta != 0) {
            target.addTrustScore(targetDelta);
            auditLogPort.record(AuditEvent.of(
                    AuditCategory.TRUST_SCORE, trustScoreAction(targetDelta), "SYSTEM", null,
                    "MEMBER", targetId.toString(), AuditResult.SUCCESS,
                    Map.of("delta", targetDelta, "reason", trustScoreReason(targetDelta), "applicationId", applicationId)));
        }
        memberOutPort.save(target);
        notifyReviewRequest(application, reviewer);
        return new ReviewResult(applicationId, "reviewed");
    }

    private String trustScoreAction(int delta) {
        return delta > 0 ? "TRUST_SCORE_ADD" : "TRUST_SCORE_DEDUCT";
    }

    private String trustScoreReason(int delta) {
        return delta > 0 ? "RATING_RECEIVED" : "LOW_RATING_RECEIVED";
    }

    private void notifyReviewRequest(Application application, Member reviewer) {
        Long targetId = application.getMatchedMemberId();
        applicationOutPort.findByMemberIdAndGameId(targetId, application.getGameId())
                .filter(opponent -> !reviewOutPort.existsByMatchIdAndReviewerId(
                        opponent.getChatId(), targetId))
                .ifPresent(opponent -> notificationUseCase.createAndPush(
                        targetId,
                        "review",
                        "동행 후기를 남겨주세요",
                        reviewer.getNickname() + "님과의 직관은 어떠셨나요?",
                        "review",
                        opponent.getId(),
                        null));
    }

    private Member findMember(Long memberId) {
        return memberOutPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
