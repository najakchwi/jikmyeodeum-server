package com.sportsmate.server.domain.application.policy;

import java.time.LocalDateTime;

public class CancellationPenaltyPolicy {
    private static final int PERIOD_DAYS = 30;
    private static final int PENALTY_THRESHOLD = 3;
    private static final int PENALTY_POINTS = 10;
    private static final int MATCHED_CANCEL_PENALTY_POINTS = 3;

    public LocalDateTime since(LocalDateTime now) {
        return now.minusDays(PERIOD_DAYS);
    }

    public int penaltyPoints(long cancellationCountIncludingCurrent) {
        return cancellationCountIncludingCurrent >= PENALTY_THRESHOLD ? -PENALTY_POINTS : 0;
    }

    public int matchedCancelPenaltyPoints() {
        return -MATCHED_CANCEL_PENALTY_POINTS;
    }
}
