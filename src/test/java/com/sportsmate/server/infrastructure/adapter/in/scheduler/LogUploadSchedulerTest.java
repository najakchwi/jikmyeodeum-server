package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.sportsmate.server.infrastructure.adapter.out.storage.log.LogUploadService;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

@DisplayName("LogUploadScheduler 단위 테스트")
class LogUploadSchedulerTest {

    @TempDir
    Path logDir;

    @Test
    @DisplayName("스케줄이 실행되면 LogUploadService에 위임한다")
    void uploadChangedLogs_delegatesToLogUploadService() {
        var logUploadService = Mockito.mock(LogUploadService.class);
        var scheduler = new LogUploadScheduler(logUploadService, logDir);

        scheduler.uploadChangedLogs();

        verify(logUploadService).uploadChangedLogs(logDir);
        assertThat(logDir).exists();
    }
}
