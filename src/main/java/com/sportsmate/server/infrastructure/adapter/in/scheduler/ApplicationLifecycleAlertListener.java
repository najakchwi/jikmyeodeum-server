package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import java.util.Map;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ApplicationLifecycleAlertListener {

    private final OpsAlertPort opsAlertPort;

    public ApplicationLifecycleAlertListener(OpsAlertPort opsAlertPort) {
        this.opsAlertPort = opsAlertPort;
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        opsAlertPort.notify(AlertSeverity.INFO, new AlertMessage(
                "앱 기동",
                "서버가 요청을 받을 준비를 마쳤습니다.",
                Map.of("event", "ApplicationReadyEvent"),
                "lifecycle:ready"));
    }

    @EventListener
    public void onClosed(ContextClosedEvent event) {
        opsAlertPort.notify(AlertSeverity.INFO, new AlertMessage(
                "앱 종료",
                "서버 컨텍스트가 종료됩니다.",
                Map.of("event", "ContextClosedEvent"),
                null));
    }
}
