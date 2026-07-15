package com.sportsmate.server.domain.application.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sportsmate.server.domain.application.matching.filter.AgePreferenceFilter;
import com.sportsmate.server.domain.application.matching.filter.DistanceFilter;
import com.sportsmate.server.domain.application.matching.filter.GenderPreferenceFilter;
import com.sportsmate.server.domain.application.matching.filter.NotPreviouslyRejectedFilter;
import com.sportsmate.server.domain.application.matching.filter.SmokingPreferenceFilter;
import com.sportsmate.server.domain.application.matching.scorer.DistanceScorer;
import com.sportsmate.server.domain.application.matching.scorer.PersonalityScorer;
import com.sportsmate.server.domain.application.matching.scorer.TeamScorer;
import com.sportsmate.server.domain.application.matching.scorer.TrustScoreScorer;
import com.sportsmate.server.domain.application.matching.scorer.WatchStyleScorer;
import com.sportsmate.server.domain.member.enums.AgePref;
import com.sportsmate.server.domain.member.enums.GenderPref;
import com.sportsmate.server.domain.member.enums.Personality;
import com.sportsmate.server.domain.member.enums.SmokingPref;
import com.sportsmate.server.domain.member.enums.SmokingStatus;
import com.sportsmate.server.domain.member.enums.TalkStyle;
import com.sportsmate.server.domain.member.enums.WatchStyle;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GreedyMatchingEngine 단위 테스트")
class GreedyMatchingEngineTest {

    private final GreedyMatchingEngine engine = new GreedyMatchingEngine(
            List.of(new GenderPreferenceFilter(), new AgePreferenceFilter(), new SmokingPreferenceFilter(),
                    new DistanceFilter(), new NotPreviouslyRejectedFilter()),
            List.of(new TrustScoreScorer(), new TeamScorer(), new WatchStyleScorer(),
                    new PersonalityScorer(), new DistanceScorer()),
            List.of(new TrustScoreScorer()));
    private final MatchWeights weights = new MatchWeights(Map.of(
            "trust", 20.0,
            "team", 40.0,
            "watchStyle", 20.0,
            "personality", 10.0,
            "distance", 10.0), 60);

    @Test
    @DisplayName("하드 필터를 통과한 후보 중 점수가 가장 높은 쌍부터 매칭한다")
    void match_eligibleCandidates_pairsByHighestScore() {
        List<MatchPair> pairs = engine.match(List.of(
                candidate("1", 1L, "LG", 100, Set.of()),
                candidate("2", 2L, "LG", 100, Set.of()),
                candidate("3", 3L, "KIA", 100, Set.of())), weights);

        assertThat(pairs).hasSize(1);
        assertThat(pairs.get(0).applicationAId()).isEqualTo("1");
        assertThat(pairs.get(0).applicationBId()).isEqualTo("2");
        assertThat(pairs.get(0).score()).isGreaterThanOrEqualTo(90);
    }

    @Test
    @DisplayName("하드 필터 위반 쌍은 후보에서 제외한다")
    void match_hardFilterViolation_excludesPair() {
        MatchCandidate male = candidate("1", 1L, "LG", 100, Set.of(), "male", GenderPref.SAME);
        MatchCandidate female = candidate("2", 2L, "LG", 100, Set.of(), "female", GenderPref.SAME);

        List<MatchPair> pairs = engine.match(List.of(male, female), weights);

        assertThat(pairs).isEmpty();
    }

    @Test
    @DisplayName("임계값 미만 쌍은 매칭하지 않는다")
    void match_belowMinimumScore_doesNotPair() {
        MatchWeights strictWeights = new MatchWeights(weights.values(), 95);

        List<MatchPair> pairs = engine.match(List.of(
                candidate("1", 1L, "LG", 30, Set.of()),
                candidate("2", 2L, "KIA", 30, Set.of())), strictWeights);

        assertThat(pairs).isEmpty();
    }

    @Test
    @DisplayName("신뢰도 가중치가 없으면 실패한다")
    void match_missingTrustWeight_throwsException() {
        MatchWeights invalidWeights = new MatchWeights(Map.of("team", 40.0), 60);

        assertThatThrownBy(() -> engine.match(List.of(
                candidate("1", 1L, "LG", 100, Set.of()),
                candidate("2", 2L, "LG", 100, Set.of())), invalidWeights))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trust");
    }

    @Test
    @DisplayName("거절 이력이 있는 쌍은 다시 매칭하지 않는다")
    void match_previouslyRejectedPair_doesNotPair() {
        List<MatchPair> pairs = engine.match(List.of(
                candidate("1", 1L, "LG", 100, Set.of(2L)),
                candidate("2", 2L, "LG", 100, Set.of())), weights);

        assertThat(pairs).isEmpty();
    }

    private MatchCandidate candidate(String applicationId, Long memberId, String team, int trustScore,
            Set<Long> rejectedMemberIds) {
        return candidate(applicationId, memberId, team, trustScore, rejectedMemberIds, "male", GenderPref.ANY);
    }

    private MatchCandidate candidate(String applicationId, Long memberId, String team, int trustScore,
            Set<Long> rejectedMemberIds, String gender, GenderPref genderPref) {
        return new MatchCandidate(
                applicationId,
                memberId,
                team,
                List.of(WatchStyle.CHEER),
                Personality.TENSION,
                TalkStyle.TALKATIVE,
                SmokingStatus.NON_SMOKER,
                gender,
                LocalDate.of(1997, 1, 1),
                null,
                null,
                5,
                genderPref,
                AgePref.ANY,
                SmokingPref.ANY,
                trustScore,
                rejectedMemberIds);
    }
}
