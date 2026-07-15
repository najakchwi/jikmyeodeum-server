package com.sportsmate.server.domain.application;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class Application {
    private String id;
    private Long memberId;
    private String gameId;
    private String status;
    private LocalDateTime appliedAt;
    private Long matchedMemberId;
    private String chatId;
    private LocalDateTime matchedAt;
    private LocalDateTime expiresAt;
    private String response;
    private LocalDateTime cancelledAt;
    private Integer matchScore;
    private Set<Long> rejectedMemberIds = new LinkedHashSet<>();

    public static Application create(String id, Long memberId, String gameId) {
        Application application = new Application();
        application.id = id;
        application.memberId = memberId;
        application.gameId = gameId;
        application.status = "waiting";
        application.appliedAt = LocalDateTime.now();
        return application;
    }

    public static Application reconstitute(String id, Long memberId, String gameId, String status,
            LocalDateTime appliedAt, Long matchedMemberId, String chatId, LocalDateTime matchedAt,
            LocalDateTime expiresAt, String response, LocalDateTime cancelledAt, Integer matchScore,
            Set<Long> rejectedMemberIds) {
        Application application = new Application();
        application.id = id;
        application.memberId = memberId;
        application.gameId = gameId;
        application.status = status;
        application.appliedAt = appliedAt;
        application.matchedMemberId = matchedMemberId;
        application.chatId = chatId;
        application.matchedAt = matchedAt;
        application.expiresAt = expiresAt;
        application.response = response;
        application.cancelledAt = cancelledAt;
        application.matchScore = matchScore;
        application.rejectedMemberIds = rejectedMemberIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(rejectedMemberIds);
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
        chatId = null;
    }

    public void assign(Long opponentId) {
        assign(opponentId, null);
    }

    public void assign(Long opponentId, Integer score) {
        matchedMemberId = opponentId;
        matchedAt = LocalDateTime.now();
        expiresAt = matchedAt.plusHours(23);
        status = "matched";
        matchScore = score;
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
    public String getStatus() { return status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public Long getMatchedMemberId() { return matchedMemberId; }
    public String getChatId() { return chatId; }
    public LocalDateTime getMatchedAt() { return matchedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getResponse() { return response; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public Integer getMatchScore() { return matchScore; }
    public Set<Long> getRejectedMemberIds() { return Set.copyOf(rejectedMemberIds); }
}
