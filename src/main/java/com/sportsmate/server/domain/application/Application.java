package com.sportsmate.server.domain.application;

import com.sportsmate.server.domain.application.matching.MatchReason;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Application {
    private String id;
    private Long memberId;
    private String gameId;
    private LocalDate gameDate;
    private String status;
    private LocalDateTime appliedAt;
    private Long matchedMemberId;
    private String chatId;
    private LocalDateTime matchedAt;
    private LocalDateTime expiresAt;
    private String response;
    private LocalDateTime cancelledAt;
    private Integer matchScore;
    private List<MatchReason> matchReasons = List.of();
    private Set<Long> rejectedMemberIds = new LinkedHashSet<>();
    private Long version;

    public static Application create(String id, Long memberId, String gameId) {
        return create(id, memberId, gameId, null);
    }

    public static Application create(String id, Long memberId, String gameId, LocalDate gameDate) {
        Application application = new Application();
        application.id = id;
        application.memberId = memberId;
        application.gameId = gameId;
        application.gameDate = gameDate;
        application.status = "waiting";
        application.appliedAt = LocalDateTime.now();
        return application;
    }

    public static Application reconstitute(String id, Long memberId, String gameId, String status,
            LocalDateTime appliedAt, Long matchedMemberId, String chatId, LocalDateTime matchedAt,
            LocalDateTime expiresAt, String response, LocalDateTime cancelledAt, Integer matchScore,
            Set<Long> rejectedMemberIds) {
        return reconstitute(id, memberId, gameId, status, appliedAt, matchedMemberId, chatId, matchedAt,
                expiresAt, response, cancelledAt, matchScore, rejectedMemberIds, null);
    }

    public static Application reconstitute(String id, Long memberId, String gameId, String status,
            LocalDateTime appliedAt, Long matchedMemberId, String chatId, LocalDateTime matchedAt,
            LocalDateTime expiresAt, String response, LocalDateTime cancelledAt, Integer matchScore,
            Set<Long> rejectedMemberIds, Long version) {
        return reconstitute(id, memberId, gameId, null, status, appliedAt, matchedMemberId, chatId, matchedAt,
                expiresAt, response, cancelledAt, matchScore, List.of(), rejectedMemberIds, version);
    }

    public static Application reconstitute(String id, Long memberId, String gameId, LocalDate gameDate,
            String status, LocalDateTime appliedAt, Long matchedMemberId, String chatId, LocalDateTime matchedAt,
            LocalDateTime expiresAt, String response, LocalDateTime cancelledAt, Integer matchScore,
            Set<Long> rejectedMemberIds, Long version) {
        return reconstitute(id, memberId, gameId, gameDate, status, appliedAt, matchedMemberId, chatId, matchedAt,
                expiresAt, response, cancelledAt, matchScore, List.of(), rejectedMemberIds, version);
    }

    public static Application reconstitute(String id, Long memberId, String gameId, LocalDate gameDate,
            String status, LocalDateTime appliedAt, Long matchedMemberId, String chatId, LocalDateTime matchedAt,
            LocalDateTime expiresAt, String response, LocalDateTime cancelledAt, Integer matchScore,
            List<MatchReason> matchReasons, Set<Long> rejectedMemberIds, Long version) {
        Application application = new Application();
        application.id = id;
        application.memberId = memberId;
        application.gameId = gameId;
        application.gameDate = gameDate;
        application.status = status;
        application.appliedAt = appliedAt;
        application.matchedMemberId = matchedMemberId;
        application.chatId = chatId;
        application.matchedAt = matchedAt;
        application.expiresAt = expiresAt;
        application.response = response;
        application.cancelledAt = cancelledAt;
        application.matchScore = matchScore;
        application.matchReasons = matchReasons == null ? List.of() : List.copyOf(matchReasons);
        application.rejectedMemberIds = rejectedMemberIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(rejectedMemberIds);
        application.version = version;
        return application;
    }

    public void cancel() {
        if (!"waiting".equals(status) && !"matched".equals(status) && !"chatting".equals(status))
            throw new IllegalStateException("CANNOT_CANCEL");
        status = "cancelled";
        cancelledAt = LocalDateTime.now();
    }

    public void resetToWaiting() {
        status = "waiting";
        matchedMemberId = null;
        matchedAt = null;
        expiresAt = null;
        response = null;
        matchScore = null;
        matchReasons = List.of();
        chatId = null;
    }

    public void assign(Long opponentId) {
        assign(opponentId, null);
    }

    public void assign(Long opponentId, Integer score) {
        assign(opponentId, score, List.of());
    }

    public void assign(Long opponentId, Integer score, List<MatchReason> reasons) {
        matchedMemberId = opponentId;
        matchedAt = LocalDateTime.now();
        expiresAt = matchedAt.plusHours(23);
        status = "matched";
        matchScore = score;
        matchReasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public void markAccepted() {
        response = "accepted";
    }

    public void confirm(String newChatId) {
        status = "chatting";
        chatId = newChatId;
    }

    public void reject() {
        if (matchedMemberId != null) {
            rejectedMemberIds.add(matchedMemberId);
        }
        response = "rejected";
        status = "waiting";
        matchedMemberId = null;
        matchedAt = null;
        expiresAt = null;
        matchScore = null;
        matchReasons = List.of();
    }

    public void completeGame() {
        if ("chatting".equals(status)) status = "game_done";
    }

    public void review() {
        if (!"game_done".equals(status)) throw new IllegalStateException("NOT_GAME_DONE");
        status = "reviewed";
    }

    public String getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getGameId() { return gameId; }
    public LocalDate getGameDate() { return gameDate; }
    public String getStatus() { return status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public Long getMatchedMemberId() { return matchedMemberId; }
    public String getChatId() { return chatId; }
    public LocalDateTime getMatchedAt() { return matchedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getResponse() { return response; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public Integer getMatchScore() { return matchScore; }
    public List<MatchReason> getMatchReasons() { return List.copyOf(matchReasons); }
    public Set<Long> getRejectedMemberIds() { return Set.copyOf(rejectedMemberIds); }
    public Long getVersion() { return version; }
}
