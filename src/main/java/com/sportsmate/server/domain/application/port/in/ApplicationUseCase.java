package com.sportsmate.server.domain.application.port.in;

import com.sportsmate.server.domain.game.port.in.GameUseCase.GameResult;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import com.sportsmate.server.domain.review.port.dto.ReviewDetail;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ApplicationUseCase {
    ApplicationResult apply(Long memberId, String gameId);
    List<ApplicationResult> applications(Long memberId, LocalDate date, List<String> statuses);
    List<LocalDate> calendar(Long memberId, int year, int month);
    ApplicationResult get(Long memberId, String applicationId);
    void cancel(Long memberId, String applicationId);
    MatchStatusResult status(Long memberId, String applicationId);
    ApplicationResult accept(Long memberId, String applicationId);
    ApplicationResult reject(Long memberId, String applicationId);
    MatchBatchResult matchWaitingApplications();
    void cancelAllActiveByMember(Long memberId);

    record ApplicationResult(
            String id, GameResult game, LocalDateTime appliedAt, String status,
            MemberProfile matchedProfile, String chatId, LocalDateTime matchedAt,
            LocalDateTime expiresAt, List<WaitingParticipantPreview> waitingPreview,
            ReviewDetail myReview) {}
    record MatchStatusResult(String status, MemberProfile matchedProfile, String chatId,
            LocalDateTime matchedAt, LocalDateTime expiresAt) {}
    record WaitingParticipantPreview(String nickname, String avatarUrl, String avatarColor) {}
    record MatchBatchResult(int gamesProcessed, int pairsMatched) {}
}
