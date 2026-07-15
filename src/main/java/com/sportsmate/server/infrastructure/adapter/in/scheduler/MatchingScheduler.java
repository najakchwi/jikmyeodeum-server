package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.domain.application.port.in.ApplicationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MatchingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchingScheduler.class);

    private final ApplicationUseCase applicationUseCase;

    public MatchingScheduler(ApplicationUseCase applicationUseCase) {
        this.applicationUseCase = applicationUseCase;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void matchWaitingApplications() {
        var result = applicationUseCase.matchWaitingApplications();
        log.info(
                "Waiting applications matched. gamesProcessed={}, pairsMatched={}",
                result.gamesProcessed(),
                result.pairsMatched());
    }
}
