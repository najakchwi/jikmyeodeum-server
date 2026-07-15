package com.sportsmate.server.domain.application.service;

import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.matching.MatchCandidate;
import com.sportsmate.server.domain.application.matching.MatchCandidateFactory;
import com.sportsmate.server.domain.application.matching.MatchPair;
import com.sportsmate.server.domain.application.matching.MatchWeights;
import com.sportsmate.server.domain.application.matching.MatchingEngine;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
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
    private final MemberUseCase memberUseCase;
    private final NotificationUseCase notificationUseCase;
    private final MatchingEngine matchingEngine;
    private final MatchCandidateFactory matchCandidateFactory;
    private final MatchWeights matchWeights;

    public ApplicationMatchingBatchProcessor(ApplicationOutPort applicationOutPort, MemberUseCase memberUseCase,
            NotificationUseCase notificationUseCase, MatchingEngine matchingEngine,
            MatchCandidateFactory matchCandidateFactory, MatchWeights matchWeights) {
        this.applicationOutPort = applicationOutPort;
        this.memberUseCase = memberUseCase;
        this.notificationUseCase = notificationUseCase;
        this.matchingEngine = matchingEngine;
        this.matchCandidateFactory = matchCandidateFactory;
        this.matchWeights = matchWeights;
    }

    @Transactional
    public int matchWaitingPairs(String gameId) {
        List<Application> waiting = applicationOutPort.findWaitingByGameId(gameId);
        Map<String, Application> applicationsById = waiting.stream()
                .collect(Collectors.toMap(Application::getId, Function.identity()));
        List<MatchCandidate> candidates = candidatesFor(gameId, waiting);
        if (candidates.size() < 2) {
            return 0;
        }

        List<MatchPair> pairs = matchingEngine.match(candidates, matchWeights);
        for (MatchPair pair : pairs) {
            Application first = applicationsById.get(pair.applicationAId());
            Application second = applicationsById.get(pair.applicationBId());
            first.assign(pair.memberBId(), pair.score());
            second.assign(pair.memberAId(), pair.score());
            applicationOutPort.save(first);
            applicationOutPort.save(second);
            notificationUseCase.createAndPush(first.getMemberId(), "match", "매칭 후보가 도착했어요!",
                    "23시간 안에 매칭 결과를 확인해주세요.", "matchResult", first.getId(), null);
            notificationUseCase.createAndPush(second.getMemberId(), "match", "매칭 후보가 도착했어요!",
                    "23시간 안에 매칭 결과를 확인해주세요.", "matchResult", second.getId(), null);
        }
        return pairs.size();
    }

    private List<MatchCandidate> candidatesFor(String gameId, List<Application> waiting) {
        List<MatchCandidate> candidates = new ArrayList<>();
        for (Application application : waiting) {
            try {
                MemberProfile profile = memberUseCase.get(application.getMemberId());
                candidates.add(matchCandidateFactory.from(application, profile));
            } catch (RuntimeException exception) {
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
        return candidates;
    }
}
