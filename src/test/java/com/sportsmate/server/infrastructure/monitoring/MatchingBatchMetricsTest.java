package com.sportsmate.server.infrastructure.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.domain.application.port.in.ApplicationUseCase.MatchBatchResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("매칭 배치 Micrometer 지표 단위 테스트")
class MatchingBatchMetricsTest {

    @Test
    @DisplayName("성공한 배치 결과를 누적 지표와 소요 시간으로 기록한다")
    void recordSuccess_recordsBatchMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MatchingBatchMetrics metrics = new MatchingBatchMetrics(registry);

        metrics.recordSuccess(new MatchBatchResult(3, 1, 0, 4, 12, 4, 2, 3, 250));

        assertThat(registry.get("matching.batch.executions").tag("outcome", "success").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("matching.batch.duration").timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isEqualTo(250);
        assertThat(registry.get("matching.batch.pairs").counter().count()).isEqualTo(4);
        assertThat(registry.get("matching.batch.applicants").counter().count()).isEqualTo(12);
        assertThat(registry.get("matching.batch.unmatched.people").counter().count()).isEqualTo(4);
        assertThat(registry.get("matching.batch.games.failed").counter().count()).isEqualTo(1);
        assertThat(registry.get("matching.batch.person.errors").counter().count()).isEqualTo(2);
        assertThat(registry.get("matching.batch.carry.over").counter().count()).isEqualTo(3);
    }

    @Test
    @DisplayName("치명 실패는 실패 실행 지표로 기록한다")
    void recordFailure_recordsFailedExecution() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MatchingBatchMetrics metrics = new MatchingBatchMetrics(registry);

        metrics.recordFailure();

        assertThat(registry.get("matching.batch.executions").tag("outcome", "failure").counter().count())
                .isEqualTo(1);
    }
}
