package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import com.sportsmate.server.infrastructure.monitoring.SafetySignalMonitor;
import com.sportsmate.server.infrastructure.monitoring.SmsUsageMonitor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class BusinessMonitoringScheduler {

    private final SafetySignalMonitor safetySignalMonitor;
    private final SmsUsageMonitor smsUsageMonitor;
    private final OpsAlertPort opsAlertPort;

    public BusinessMonitoringScheduler(
            SafetySignalMonitor safetySignalMonitor,
            SmsUsageMonitor smsUsageMonitor,
            OpsAlertPort opsAlertPort) {
        this.safetySignalMonitor = safetySignalMonitor;
        this.smsUsageMonitor = smsUsageMonitor;
        this.opsAlertPort = opsAlertPort;
    }

    @Scheduled(fixedRateString = "${app.alert.business.check-rate-ms:60000}")
    public void checkBusinessSignals() {
        safetySignalMonitor.check();
        smsUsageMonitor.check();
    }

    @Scheduled(cron = "${app.alert.business.daily-digest-cron:0 0 10 * * *}", zone = "Asia/Seoul")
    public void sendDailyDigest() {
        Map<String, String> fields = new LinkedHashMap<>();
        addSmsDigest(fields);
        addSafetyDigest(fields);
        fields.put("M2 가입 퍼널", "집계 테이블 없음");
        fields.put("M3 활동 지표", "집계 테이블 없음");
        fields.put("M5 별점 분포", "집계 테이블 없음");
        opsAlertPort.notify(AlertSeverity.METRIC, new AlertMessage(
                "일일 운영 다이제스트",
                "Daily business, SMS, and safety metrics digest.",
                fields,
                null));
    }

    private void addSmsDigest(Map<String, String> fields) {
        try {
            var sms = smsUsageMonitor.snapshot();
            fields.put("M4 SMS 발송", String.valueOf(sms.sent()));
            fields.put("M4 SMS 성공률", sms.successRate());
            fields.put("W9 SMS 차단", String.valueOf(sms.blocked()));
        } catch (RuntimeException exception) {
            fields.put("M4 SMS", "조회 실패");
        }
    }

    private void addSafetyDigest(Map<String, String> fields) {
        try {
            var safety = safetySignalMonitor.snapshot();
            fields.put("M5 신고", String.valueOf(safety.reports()));
            fields.put("C7 탈퇴", String.valueOf(safety.withdrawals()));
        } catch (RuntimeException exception) {
            fields.put("M5 안전", "조회 실패");
        }
    }
}
