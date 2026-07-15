package com.sportsmate.server.common.port.out.alert;

public interface OpsAlertPort {
    void notify(AlertSeverity severity, AlertMessage message);
    void resolve(String dedupeKey);
}
