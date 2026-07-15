package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.domain.game.port.in.SyncSeasonScheduleUseCase;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class GameScheduleSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(GameScheduleSyncScheduler.class);

    private final SyncSeasonScheduleUseCase syncSeasonScheduleUseCase;

    public GameScheduleSyncScheduler(SyncSeasonScheduleUseCase syncSeasonScheduleUseCase) {
        this.syncSeasonScheduleUseCase = syncSeasonScheduleUseCase;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void syncCurrentAndNextMonth() {
        YearMonth currentMonth = YearMonth.now();
        sync(currentMonth);
        sync(currentMonth.plusMonths(1));
    }

    private void sync(YearMonth month) {
        var result = syncSeasonScheduleUseCase.sync(month);
        log.info(
                "KBO schedule synced. month={}, created={}, updated={}, cancelled={}",
                month,
                result.created(),
                result.updated(),
                result.cancelled()
        );
    }
}
