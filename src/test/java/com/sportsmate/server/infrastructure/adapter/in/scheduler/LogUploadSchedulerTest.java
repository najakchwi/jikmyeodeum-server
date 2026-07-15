package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.common.port.out.storage.StoredObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("LogUploadScheduler 단위 테스트")
class LogUploadSchedulerTest {

    @TempDir
    Path logDir;

    @Test
    @DisplayName("audit/api 로그만 R2 logs 프리픽스로 업로드한다")
    void uploadChangedLogs_withEligibleFiles_uploadsOnlyAuditAndApiLogs() throws IOException {
        writeLog("audit.log", "audit");
        writeLog("api.log", "api");
        writeLog("app.log", "app");
        writeLog("audit.txt", "audit-text");
        var objectStorage = new FakeObjectStorage();
        var scheduler = new LogUploadScheduler(objectStorage, logDir);

        scheduler.uploadChangedLogs();

        assertThat(objectStorage.uploads)
                .extracting(UploadedObject::objectKey)
                .containsExactly("logs/api.log", "logs/audit.log");
        assertThat(objectStorage.uploads)
                .extracting(UploadedObject::contentType)
                .containsOnly("text/plain");
    }

    @Test
    @DisplayName("mtime이 바뀌지 않은 로그는 다시 업로드하지 않는다")
    void uploadChangedLogs_withUnchangedMtime_skipsAlreadyUploadedFile() throws IOException {
        Path apiLog = writeLog("api.log", "api");
        var objectStorage = new FakeObjectStorage();
        var scheduler = new LogUploadScheduler(objectStorage, logDir);

        scheduler.uploadChangedLogs();
        scheduler.uploadChangedLogs();
        Files.writeString(apiLog, "api-updated", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(apiLog, FileTime.fromMillis(System.currentTimeMillis() + 10_000));
        scheduler.uploadChangedLogs();

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
        var scheduler = new LogUploadScheduler(objectStorage, logDir);

        scheduler.uploadChangedLogs();

        assertThat(objectStorage.uploads)
                .extracting(UploadedObject::objectKey)
                .containsExactly("logs/audit.log");
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
