package com.sportsmate.server.domain.application.service;

import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.application.matching.MatchCandidateFactory;
import com.sportsmate.server.domain.application.matching.MatchPair;
import com.sportsmate.server.domain.application.matching.MatchWeights;
import com.sportsmate.server.domain.application.matching.MatchingEngine;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.domain.member.port.in.MemberProfile;
import com.sportsmate.server.domain.member.port.in.MemberUseCase;
import com.sportsmate.server.domain.notification.port.in.NotificationUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationMatchingBatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(ApplicationMatchingBatchProcessor.class);

    private final ApplicationOutPort applicationOutPort;
    private final GameOutPort gameOutPort;
    private final MemberUseCase memberUseCase;
    private final NotificationUseCase notificationUseCase;
    private final MatchingEngine matchingEngine;
    private final MatchCandidateFactory matchCandidateFactory;
    private final MatchWeights matchWeights;

    public ApplicationMatchingBatchProcessor(ApplicationOutPort applicationOutPort, GameOutPort gameOutPort,
            MemberUseCase memberUseCase, NotificationUseCase notificationUseCase, MatchingEngine matchingEngine,
            MatchCandidateFactory matchCandidateFactory, MatchWeights matchWeights) {
        this.applicationOutPort = applicationOutPort;
        this.gameOutPort = gameOutPort;
        this.memberUseCase = memberUseCase;
        this.notificationUseCase = notificationUseCase;
        this.matchingEngine = matchingEngine;
        this.matchCandidateFactory = matchCandidateFactory;
        this.matchWeights = matchWeights;
    }

    @Transactional
    public GameMatchBatchResult matchWaitingPairs(String gameId) {
        List<Application> waiting = applicationOutPort.findWaitingByGameId(gameId);
        Map<String, Application> applicationsById = waiting.stream()
                .collect(Collectors.toMap(Application::getId, Function.identity()));
        Long leagueId = gameOutPort.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId))
                .leagueId();
        CandidateBatch candidates = candidatesFor(gameId, leagueId, waiting);
        if (candidates.size() < 2) {
            return new GameMatchBatchResult(waiting.size(), 0, candidates.personErrors(), waiting.size());
        }

        List<MatchPair> pairs = matchingEngine.match(candidates.items(), matchWeights);
        for (MatchPair pair : pairs) {
            Application first = applicationsById.get(pair.applicationAId());
            Application second = applicationsById.get(pair.applicationBId());
            first.assign(pair.memberBId(), pair.score(), pair.reasons());
            second.assign(pair.memberAId(), pair.score(), pair.reasons());
            applicationOutPort.save(first);
            applicationOutPort.save(second);
            notificationUseCase.createAndPush(first.getMemberId(), "match", "매칭 후보가 도착했어요!",
                    "23시간 안에 매칭 결과를 확인해주세요.", "matchResult", first.getId(), null);
            notificationUseCase.createAndPush(second.getMemberId(), "match", "매칭 후보가 도착했어요!",
                    "23시간 안에 매칭 결과를 확인해주세요.", "matchResult", second.getId(), null);
        }
        int carryOver = (int) waiting.stream().filter(application -> "waiting".equals(application.getStatus())).count();
        return new GameMatchBatchResult(waiting.size(), pairs.size(), candidates.personErrors(), carryOver);
    }

    private CandidateBatch candidatesFor(String gameId, Long leagueId, List<Application> waiting) {
        List<MatchCandidate> candidates = new ArrayList<>();
        int personErrors = 0;
        for (Application application : waiting) {
            try {
                MemberProfile profile = memberUseCase.get(application.getMemberId());
                candidates.add(matchCandidateFactory.from(application, profile, leagueId));
            } catch (RuntimeException exception) {
                personErrors++;
                // Log only: changing orphan waiting application status belongs to the planning state machine.
                log.warn(
                        "Waiting application excluded from matching. gameId={}, applicationId={}, memberId={}, reason={}",
                        gameId,
                        application.getId(),
                        application.getMemberId(),
                        exception.getMessage(),
                        exception);
            }
        }
        return new CandidateBatch(candidates, personErrors);
    }

    public record GameMatchBatchResult(int totalApplicants, int pairsMatched, int personErrors, int carryOver) {
    }

    private record CandidateBatch(List<MatchCandidate> items, int personErrors) {
        int size() {
            return items.size();
        }
    }
}
