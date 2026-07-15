package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import com.sportsmate.server.infrastructure.adapter.out.storage.log.LogUploadService;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class LogUploadScheduler {

    private final LogUploadService logUploadService;
    private final Path logDir;
    private final OpsAlertPort opsAlertPort;
    private final JobHeartbeat jobHeartbeat;

    @Autowired
    public LogUploadScheduler(LogUploadService logUploadService, OpsAlertPort opsAlertPort, JobHeartbeat jobHeartbeat) {
        this(logUploadService, Path.of("logs"), opsAlertPort, jobHeartbeat);
    }

    LogUploadScheduler(LogUploadService logUploadService, Path logDir) {
        this(logUploadService, logDir, null, null);
    }

    LogUploadScheduler(LogUploadService logUploadService, Path logDir, OpsAlertPort opsAlertPort,
            JobHeartbeat jobHeartbeat) {
        this.logUploadService = logUploadService;
        this.logDir = logDir;
        this.opsAlertPort = opsAlertPort;
        this.jobHeartbeat = jobHeartbeat;
    }

    @Scheduled(fixedRate = 600_000, initialDelay = 600_000)
    public void uploadChangedLogs() {
        try {
            logUploadService.uploadChangedLogs(logDir);
            if (jobHeartbeat != null) {
                jobHeartbeat.markSuccess("log-upload");
            }
        } catch (RuntimeException exception) {
            int failures = jobHeartbeat == null ? 1 : jobHeartbeat.markFailure("log-upload");
            if (opsAlertPort != null && failures >= 3) {
                opsAlertPort.notify(AlertSeverity.WARNING, new AlertMessage(
                        "로그 업로드 반복 실패",
                        exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                        Map.of("job", "log-upload", "consecutiveFailures", String.valueOf(failures)),
                        "job:log-upload:failure"));
            }
            throw exception;
        }
    }
}
