package com.sportsmate.server.infrastructure.adapter.out.schedule;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.domain.game.port.out.GameScheduleSourcePort;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "prod"})
public class R2CsvGameScheduleSourceAdapter implements GameScheduleSourcePort {

    private static final String OBJECT_KEY_PATTERN = "schedule/kbo/kbo-%s.csv";

    private final ObjectStorage objectStorage;

    public R2CsvGameScheduleSourceAdapter(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    @Override
    public List<SourceGame> fetchMonthlySchedule(YearMonth month) {
        String objectKey = OBJECT_KEY_PATTERN.formatted(month);
        return objectStorage.download(objectKey)
                .map(this::parse)
                .orElse(List.of());
    }

    private List<SourceGame> parse(byte[] bytes) {
        try (
                var inputStream = new ByteArrayInputStream(bytes);
                var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                var parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .get()
                        .parse(reader)
        ) {
            return parser.stream()
                    .filter(this::hasRequiredValues)
                    .map(this::toSourceGame)
                    .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("KBO season schedule CSV parse failed", exception);
        }
    }

    private boolean hasRequiredValues(CSVRecord record) {
        return !value(record, "game_id").isBlank()
                && !value(record, "date").isBlank()
                && !value(record, "time").isBlank()
                && !value(record, "home_team_id").isBlank()
                && !value(record, "away_team_id").isBlank()
                && !value(record, "stadium_id").isBlank();
    }

    private SourceGame toSourceGame(CSVRecord record) {
        return new SourceGame(
                value(record, "game_id"),
                Long.valueOf(value(record, "home_team_id")),
                Long.valueOf(value(record, "away_team_id")),
                Long.valueOf(value(record, "stadium_id")),
                LocalDate.parse(value(record, "date")),
                LocalTime.parse(value(record, "time")),
                nullableInteger(record, "home_score"),
                nullableInteger(record, "away_score")
        );
    }

    private String value(CSVRecord record, String header) {
        return record.isMapped(header) ? record.get(header).trim() : "";
    }

    private Integer nullableInteger(CSVRecord record, String header) {
        String value = value(record, header);
        return value.isBlank() ? null : Integer.valueOf(value);
    }
}
