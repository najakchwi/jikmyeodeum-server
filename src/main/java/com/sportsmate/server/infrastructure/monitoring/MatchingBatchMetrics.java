package com.sportsmate.server.infrastructure.monitoring;

import com.sportsmate.server.domain.application.port.in.ApplicationUseCase.MatchBatchResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class MatchingBatchMetrics {

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter matchedPairsCounter;
    private final Counter applicantsCounter;
    private final Counter unmatchedPeopleCounter;
    private final Counter failedGamesCounter;
    private final Counter personErrorsCounter;
    private final Counter carryOverCounter;
    private final Timer durationTimer;

    public MatchingBatchMetrics(MeterRegistry meterRegistry) {
        successCounter = counter(meterRegistry, "matching.batch.executions", "outcome", "success");
        failureCounter = counter(meterRegistry, "matching.batch.executions", "outcome", "failure");
        matchedPairsCounter = counter(meterRegistry, "matching.batch.pairs");
        applicantsCounter = counter(meterRegistry, "matching.batch.applicants");
        unmatchedPeopleCounter = counter(meterRegistry, "matching.batch.unmatched.people");
        failedGamesCounter = counter(meterRegistry, "matching.batch.games.failed");
        personErrorsCounter = counter(meterRegistry, "matching.batch.person.errors");
        carryOverCounter = counter(meterRegistry, "matching.batch.carry.over");
        durationTimer = Timer.builder("matching.batch.duration")
                .description("Completed matching batch duration")
                .register(meterRegistry);
    }

    public void recordSuccess(MatchBatchResult result) {
        successCounter.increment();
        durationTimer.record(Duration.ofMillis(result.durationMs()));
        increment(matchedPairsCounter, result.pairsMatched());
        increment(applicantsCounter, result.totalApplicants());
        increment(unmatchedPeopleCounter, result.unmatchedPeople());
        increment(failedGamesCounter, result.gamesFailed());
        increment(personErrorsCounter, result.personErrors());
        increment(carryOverCounter, result.carryOver());
    }

    public void recordFailure() {
        failureCounter.increment();
    }

    private Counter counter(MeterRegistry meterRegistry, String name, String... tags) {
        return Counter.builder(name).tags(tags).register(meterRegistry);
    }

    private void increment(Counter counter, long amount) {
        if (amount > 0) {
            counter.increment(amount);
        }
    }
}
