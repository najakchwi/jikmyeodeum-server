package com.sportsmate.server.infrastructure.adapter.out.storage.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.common.port.out.storage.StoredObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("LogUploadService 단위 테스트")
class LogUploadServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path logDir;

    @Test
    @DisplayName("uploadChangedLogs는 audit/api 로그만 R2 logs 프리픽스로 업로드한다")
    void uploadChangedLogs_withEligibleFiles_uploadsOnlyAuditAndApiLogs() throws IOException {
        writeLog("audit.log", "audit");
        writeLog("api.log", "api");
        writeLog("app.log", "app");
        writeLog("audit.txt", "audit-text");
        var objectStorage = new FakeObjectStorage();
        var service = new LogUploadService(objectStorage, FIXED_CLOCK);

        service.uploadChangedLogs(logDir);

        assertThat(objectStorage.uploads)
                .extracting(UploadedObject::objectKey)
                .containsExactly("logs/api.log", "logs/audit.log");
    }

    @Test
    @DisplayName("uploadChangedLogs는 mtime이 바뀌지 않은 로그를 다시 업로드하지 않는다")
    void uploadChangedLogs_withUnchangedMtime_skipsAlreadyUploadedFile() throws IOException {
        Path apiLog = writeLog("api.log", "api");
        var objectStorage = new FakeObjectStorage();
        var service = new LogUploadService(objectStorage, FIXED_CLOCK);

        service.uploadChangedLogs(logDir);
        service.uploadChangedLogs(logDir);
        Files.writeString(apiLog, "api-updated", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(apiLog, FileTime.fromMillis(System.currentTimeMillis() + 10_000));
        service.uploadChangedLogs(logDir);

        assertThat(objectStorage.uploads)
                .extracting(UploadedObject::content)
                .containsExactly("api", "api-updated");
    }

    @Test
    @DisplayName("한 파일 업로드 실패가 다른 파일 업로드를 막지 않는다")
    void uploadChangedLogs_whenUploadFails_continuesWithOtherFiles() throws IOException {
        writeLog("api.log", "api");
        writeLog("audit.log", "audit");
        var objectStorage = new FakeObjectStorage("logs/api.log");
        var service = new LogUploadService(objectStorage, FIXED_CLOCK);

        service.uploadChangedLogs(logDir);

        assertThat(objectStorage.uploads)
                .extracting(UploadedObject::objectKey)
                .containsExactly("logs/audit.log");
    }

    @Test
    @DisplayName("uploadSelected는 선택한 타입/기간의 로그만 업로드한다")
    void uploadSelected_withTypeAndDateRange_uploadsOnlyMatchingFiles() throws IOException {
        writeLog("audit.2026-07-10.log", "audit-10");
        writeLog("audit.2026-07-14.log", "audit-14");
        writeLog("audit.2026-07-01.log", "audit-01");
        writeLog("api.2026-07-14.log", "api-14");
        writeLog("app.2026-07-14.log", "app-14");
        var objectStorage = new FakeObjectStorage();
        var service = new LogUploadService(objectStorage, FIXED_CLOCK);

        var result = service.uploadSelected(
                logDir, Set.of(LogType.AUDIT), java.time.LocalDate.of(2026, 7, 10), java.time.LocalDate.of(2026, 7, 14));

        assertThat(result.uploadedFiles()).containsExactly("audit.2026-07-10.log", "audit.2026-07-14.log");
        assertThat(result.failedFiles()).isEmpty();
    }

    @Test
    @DisplayName("uploadSelected는 오늘 날짜의 확장자 없는 라이브 로그 파일을 오늘 날짜로 취급한다")
    void uploadSelected_withLiveFile_treatsAsToday() throws IOException {
        writeLog("audit.log", "audit-live");
        var objectStorage = new FakeObjectStorage();
        var service = new LogUploadService(objectStorage, FIXED_CLOCK);

        var result = service.uploadSelected(
                logDir, Set.of(LogType.AUDIT), java.time.LocalDate.of(2026, 7, 15), java.time.LocalDate.of(2026, 7, 15));

        assertThat(result.uploadedFiles()).containsExactly("audit.log");
    }

    @Test
    @DisplayName("uploadSelected는 mtime 캐시와 무관하게 강제로 재업로드한다")
    void uploadSelected_ignoresLastUploadedMtimeCache() throws IOException {
        writeLog("audit.2026-07-14.log", "audit-14");
        var objectStorage = new FakeObjectStorage();
        var service = new LogUploadService(objectStorage, FIXED_CLOCK);

        service.uploadChangedLogs(logDir);
        var result = service.uploadSelected(
                logDir, Set.of(LogType.AUDIT), java.time.LocalDate.of(2026, 7, 14), java.time.LocalDate.of(2026, 7, 14));

        assertThat(result.uploadedFiles()).containsExactly("audit.2026-07-14.log");
        assertThat(objectStorage.uploads).hasSize(2);
    }

    private Path writeLog(String fileName, String content) throws IOException {
        Path file = logDir.resolve(fileName);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private record UploadedObject(
            String objectKey,
            String contentType,
            long contentLength,
            String content
    ) {
    }

    private static class FakeObjectStorage implements ObjectStorage {

        private final String failObjectKey;
        private final List<UploadedObject> uploads = new ArrayList<>();

        private FakeObjectStorage() {
            this(null);
        }

        private FakeObjectStorage(String failObjectKey) {
            this.failObjectKey = failObjectKey;
        }

        @Override
        public StoredObject upload(ObjectUploadCommand command) {
            if (command.objectKey().equals(failObjectKey)) {
                throw new RuntimeException("upload failed");
            }
            try {
                String content = new String(command.inputStream().readAllBytes(), StandardCharsets.UTF_8);
                uploads.add(new UploadedObject(
                        command.objectKey(),
                        command.contentType(),
                        command.contentLength(),
                        content
                ));
                return new StoredObject(command.objectKey(), "https://example.com/" + command.objectKey(),
                        command.contentType(), command.contentLength());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void delete(String objectKey) {
        }

        @Override
        public Optional<byte[]> download(String objectKey) {
            return Optional.empty();
        }

        @Override
        public String getUrl(String objectKey) {
            return "https://example.com/" + objectKey;
        }

        @Override
        public String extractKey(String url) {
            return null;
        }
    }
}
