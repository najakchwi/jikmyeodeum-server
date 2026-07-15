package com.sportsmate.server.infrastructure.monitoring;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ExternalDependencyAlertScheduler {

    private final ExternalDependencyMonitor externalDependencyMonitor;

    public ExternalDependencyAlertScheduler(ExternalDependencyMonitor externalDependencyMonitor) {
        this.externalDependencyMonitor = externalDependencyMonitor;
    }

    @Scheduled(fixedRateString = "${app.alert.external.check-rate-ms:60000}")
    public void check() {
        externalDependencyMonitor.check();
    }
}
