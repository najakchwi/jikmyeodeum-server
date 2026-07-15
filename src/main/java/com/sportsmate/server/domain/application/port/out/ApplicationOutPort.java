package com.sportsmate.server.domain.application.port.out;

import com.sportsmate.server.domain.application.Application;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationOutPort {
    Application save(Application application);
    String createMatch(Application application, Application opponent);
    String createSoloMatch(Application application);
    void addParticipant(String chatId, Application application);
    Optional<Application> findByIdAndMemberId(String id, Long memberId);
    Optional<Application> findByMemberIdAndGameId(Long memberId, String gameId);
    Optional<Application> findByChatIdAndMemberId(String chatId, Long memberId);
    List<Application> findByMemberId(Long memberId);
    List<Application> findWaitingByGameId(String gameId);
    List<String> findGameIdsWithWaitingApplications();
    boolean existsActiveByMemberIdAndGameId(Long memberId, String gameId);
    List<LocalDate> findAppliedDates(Long memberId, int year, int month);
    long countChattingCancellationsSince(Long memberId, LocalDateTime since);
}
