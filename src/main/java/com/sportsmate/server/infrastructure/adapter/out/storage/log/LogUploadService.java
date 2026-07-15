package com.sportsmate.server.infrastructure.adapter.out.storage.log;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 로컬 로그 파일(audit/api)을 R2 오브젝트 스토리지에 업로드한다.
 * 스케줄러(주기적, 변경분만)와 관리자 API(즉시, 선택한 타입/기간 강제 재업로드)가 공용으로 사용한다.
 */
@Component
public class LogUploadService {

    private static final Logger log = LoggerFactory.getLogger(LogUploadService.class);
    private static final Pattern FILE_NAME_PATTERN =
            Pattern.compile("^(?<prefix>[a-zA-Z]+)(\\.(?<date>\\d{4}-\\d{2}-\\d{2}))?\\.log$");

    private final ObjectStorage objectStorage;
    private final Clock clock;
    private final Map<String, Long> lastUploadedMtime = new ConcurrentHashMap<>();

    @Autowired
    public LogUploadService(ObjectStorage objectStorage) {
        this(objectStorage, Clock.systemDefaultZone());
    }

    LogUploadService(ObjectStorage objectStorage, Clock clock) {
        this.objectStorage = objectStorage;
        this.clock = clock;
    }

    /**
     * 마지막 업로드 이후 변경된 audit/api 로그만 업로드한다. 스케줄러용.
     */
    public void uploadChangedLogs(Path logDir) {
        for (var file : listLogFiles(logDir, Set.of(LogType.values()), null, null)) {
            long mtime = file.toFile().lastModified();
            String fileName = file.getFileName().toString();
            if (lastUploadedMtime.getOrDefault(fileName, -1L) == mtime) {
                continue;
            }
            if (uploadFile(file)) {
                lastUploadedMtime.put(fileName, mtime);
            }
        }
    }

    /**
     * 선택한 로그 타입/기간의 파일을 mtime 캐시와 무관하게 강제로 재업로드한다. 관리자 API용.
     */
    public LogUploadResult uploadSelected(Path logDir, Set<LogType> logTypes, LocalDate fromDate, LocalDate toDate) {
        var uploaded = new ArrayList<String>();
        var failed = new ArrayList<String>();

        for (var file : listLogFiles(logDir, logTypes, fromDate, toDate)) {
            String fileName = file.getFileName().toString();
            if (uploadFile(file)) {
                uploaded.add(fileName);
                lastUploadedMtime.put(fileName, file.toFile().lastModified());
            } else {
                failed.add(fileName);
            }
        }

        return new LogUploadResult(uploaded, failed);
    }

    private boolean uploadFile(Path file) {
        try (var in = Files.newInputStream(file)) {
            objectStorage.upload(new ObjectUploadCommand(
                    "logs/" + file.getFileName(),
                    "text/plain",
                    Files.size(file),
                    in
            ));
            return true;
        } catch (IOException e) {
            log.warn("log upload failed: {}", file.getFileName(), e);
            return false;
        } catch (RuntimeException e) {
            log.warn("log upload failed: {}", file.getFileName(), e);
            return false;
        }
    }

    private List<Path> listLogFiles(Path logDir, Set<LogType> logTypes, LocalDate fromDate, LocalDate toDate) {
        var files = logDir.toFile().listFiles();
        if (files == null) {
            return List.of();
        }

        var matched = new ArrayList<Path>();
        for (var file : files) {
            if (matchesSelection(file.getName(), logTypes, fromDate, toDate)) {
                matched.add(file.toPath());
            }
        }
        matched.sort((left, right) -> left.getFileName().toString().compareTo(right.getFileName().toString()));
        return matched;
    }

    private boolean matchesSelection(String fileName, Set<LogType> logTypes, LocalDate fromDate, LocalDate toDate) {
        Matcher matcher = FILE_NAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return false;
        }

        String prefix = matcher.group("prefix");
        LogType matchedType = Arrays.stream(LogType.values())
                .filter(type -> type.filePrefix().equals(prefix))
                .findFirst()
                .orElse(null);
        if (matchedType == null || !logTypes.contains(matchedType)) {
            return false;
        }

        if (fromDate == null || toDate == null) {
            return true;
        }

        LocalDate fileDate = resolveFileDate(matcher.group("date"));
        return !fileDate.isBefore(fromDate) && !fileDate.isAfter(toDate);
    }

    private LocalDate resolveFileDate(String dateGroup) {
        if (dateGroup == null) {
            return LocalDate.now(clock);
        }
        try {
            return LocalDate.parse(dateGroup);
        } catch (DateTimeParseException e) {
            return LocalDate.now(clock);
        }
    }
}
