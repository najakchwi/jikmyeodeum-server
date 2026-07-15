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
        try {
            var result = applicationUseCase.matchWaitingApplications();
            log.info(
                    "Waiting applications matched. gamesProcessed={}, gamesFailed={}, pairsMatched={}",
                    result.gamesProcessed(),
                    result.gamesFailed(),
                    result.pairsMatched());
            if (result.gamesFailed() > 0) {
                // TODO: connect an operations alert channel for partial matching batch failures.
                log.warn("Waiting applications matching completed with failed games. gamesFailed={}",
                        result.gamesFailed());
            }
        } catch (RuntimeException exception) {
            // TODO: connect an operations alert channel for fatal matching batch failures.
            log.error("Waiting applications matching failed fatally.", exception);
        }
    }
}
