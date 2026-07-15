package com.sportsmate.server.domain.game.port.in;

import java.time.YearMonth;
import java.util.List;

public interface SyncSeasonScheduleUseCase {

    SyncResult sync(YearMonth month);

    SyncRangeResult sync(YearMonth fromMonth, YearMonth toMonth);

    record SyncResult(int created, int updated, int cancelled) {
    }

    record MonthlySyncResult(YearMonth month, int created, int updated, int cancelled) {
    }

    record SyncRangeResult(
            YearMonth fromMonth,
            YearMonth toMonth,
            List<MonthlySyncResult> months,
            int created,
            int updated,
            int cancelled
    ) {
    }
}
