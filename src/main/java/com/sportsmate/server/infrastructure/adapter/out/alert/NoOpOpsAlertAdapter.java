package com.sportsmate.server.infrastructure.adapter.out.alert;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class NoOpOpsAlertAdapter implements OpsAlertPort {

    @Override
    public void notify(AlertSeverity severity, AlertMessage message) {
    }

    @Override
    public void resolve(String dedupeKey) {
    }
}
