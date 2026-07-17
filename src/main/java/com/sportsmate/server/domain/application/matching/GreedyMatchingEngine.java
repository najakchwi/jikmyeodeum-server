package com.sportsmate.server.domain.application.matching;

import com.sportsmate.server.domain.application.matching.filter.MatchFilter;
import com.sportsmate.server.domain.application.matching.scorer.CoreMatchScorer;
import com.sportsmate.server.domain.application.matching.scorer.MatchScorer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GreedyMatchingEngine implements MatchingEngine {

    private final List<MatchFilter> filters;
    private final List<MatchScorer> scorers;
    private final List<CoreMatchScorer> coreScorers;

    public GreedyMatchingEngine(List<MatchFilter> filters, List<MatchScorer> scorers,
            List<CoreMatchScorer> coreScorers) {
        this.filters = List.copyOf(filters);
        this.scorers = List.copyOf(scorers);
        this.coreScorers = List.copyOf(coreScorers);
        if (this.coreScorers.isEmpty()) {
            throw new IllegalStateException("Core match scorer is required");
        }
    }

    @Override
    public List<MatchPair> match(List<MatchCandidate> candidates, MatchWeights weights) {
        validateCoreWeights(weights);
        List<ScoredPair> scoredPairs = calculateScoredPairs(candidates, weights);
        Set<String> assignedApplicationIds = new HashSet<>();
        java.util.ArrayList<MatchPair> result = new java.util.ArrayList<>();
        for (ScoredPair pair : scoredPairs.stream()
                .sorted(Comparator.comparingInt(ScoredPair::score).reversed())
                .toList()) {
            if (assignedApplicationIds.contains(pair.a().applicationId())
                    || assignedApplicationIds.contains(pair.b().applicationId())) {
                continue;
            }
            assignedApplicationIds.add(pair.a().applicationId());
            assignedApplicationIds.add(pair.b().applicationId());
            result.add(new MatchPair(
                    pair.a().applicationId(),
                    pair.b().applicationId(),
                    pair.a().memberId(),
                    pair.b().memberId(),
                    pair.breakdown().score(),
                    pair.breakdown().reasons()));
        }
        return result;
    }

    private List<ScoredPair> calculateScoredPairs(List<MatchCandidate> candidates, MatchWeights weights) {
        java.util.ArrayList<ScoredPair> result = new java.util.ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                MatchCandidate a = candidates.get(i);
                MatchCandidate b = candidates.get(j);
                if (!isEligible(a, b)) {
                    continue;
                }
                ScoreBreakdown breakdown = score(a, b, weights);
                if (breakdown.score() >= weights.minimumScore()) {
                    result.add(new ScoredPair(a, b, breakdown));
                }
            }
        }
        return result;
    }

    private boolean isEligible(MatchCandidate a, MatchCandidate b) {
        return filters.stream().allMatch(filter -> filter.isEligible(a, b));
    }

    private ScoreBreakdown score(MatchCandidate a, MatchCandidate b, MatchWeights weights) {
        double weightedScore = 0.0;
        double totalWeight = 0.0;
        java.util.ArrayList<MatchReason> reasons = new java.util.ArrayList<>();
        for (MatchScorer scorer : scorers) {
            double weight = weights.weightFor(scorer.key());
            if (weight <= 0.0) {
                continue;
            }
            double contribution = clamp(scorer.score(a, b)) * weight;
            weightedScore += contribution;
            totalWeight += weight;
            if (contribution > 0.0) {
                reasons.add(new MatchReason(scorer.key(), contribution));
            }
        }
        if (totalWeight == 0.0) {
            return new ScoreBreakdown(0, List.of());
        }
        return new ScoreBreakdown((int) Math.round((weightedScore / totalWeight) * 100.0), reasons);
    }

    private void validateCoreWeights(MatchWeights weights) {
        for (CoreMatchScorer coreScorer : coreScorers) {
            if (weights.weightFor(coreScorer.key()) <= 0.0) {
                throw new IllegalStateException("Core match scorer weight must be positive: " + coreScorer.key());
            }
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record ScoreBreakdown(int score, List<MatchReason> reasons) {
        private ScoreBreakdown {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }
    }

    private record ScoredPair(MatchCandidate a, MatchCandidate b, ScoreBreakdown breakdown) {
        private int score() {
            return breakdown.score();
        }
    }
}
