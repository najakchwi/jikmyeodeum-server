package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class LogUploadScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogUploadScheduler.class);

    private final ObjectStorage objectStorage;
    private final Path logDir;
    private final Map<String, Long> lastUploadedMtime = new ConcurrentHashMap<>();

    public LogUploadScheduler(ObjectStorage objectStorage) {
        this(objectStorage, Path.of("logs"));
    }

    LogUploadScheduler(ObjectStorage objectStorage, Path logDir) {
        this.objectStorage = objectStorage;
        this.logDir = logDir;
    }

    @Scheduled(fixedRate = 600_000)
    public void uploadChangedLogs() {
        var logDirFile = logDir.toFile();
        var files = logDirFile.listFiles((dir, name) ->
                (name.startsWith("audit") || name.startsWith("api")) && name.endsWith(".log"));
        if (files == null) {
            return;
        }

        Arrays.sort(files, (left, right) -> left.getName().compareTo(right.getName()));

        for (var file : files) {
            long mtime = file.lastModified();
            if (lastUploadedMtime.getOrDefault(file.getName(), -1L) == mtime) {
                continue;
            }

            try (var in = Files.newInputStream(file.toPath())) {
                objectStorage.upload(new ObjectUploadCommand(
                        "logs/" + file.getName(),
                        "text/plain",
                        file.length(),
                        in
                ));
                lastUploadedMtime.put(file.getName(), mtime);
            } catch (IOException e) {
                log.warn("log upload failed: {}", file.getName(), e);
            } catch (RuntimeException e) {
                log.warn("log upload failed: {}", file.getName(), e);
            }
        }
    }
}
