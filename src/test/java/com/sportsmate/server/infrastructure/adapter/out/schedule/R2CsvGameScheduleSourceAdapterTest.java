package com.sportsmate.server.infrastructure.adapter.out.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.common.port.out.storage.StoredObject;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("R2CsvGameScheduleSourceAdapter 단위 테스트")
class R2CsvGameScheduleSourceAdapterTest {

    private final FakeObjectStorage objectStorage = new FakeObjectStorage();
    private final R2CsvGameScheduleSourceAdapter adapter = new R2CsvGameScheduleSourceAdapter(objectStorage);

    @Test
    @DisplayName("월별 CSV를 읽어 경기 일정 원천 데이터로 변환한다")
    void fetchMonthlySchedule_withCsv_parsesRows() {
        objectStorage.bytes = """
                game_id,date,time,home_team_id,away_team_id,stadium_id,home_score,away_score
                2026-0001,2026-03-28,18:30,1,8,1,,
                2026-0002,2026-03-29,14:00,4,2,3,5,3
                """.getBytes(StandardCharsets.UTF_8);

        var games = adapter.fetchMonthlySchedule(YearMonth.of(2026, 3));

        assertThat(objectStorage.requestedKey).isEqualTo("schedule/kbo/kbo-2026-03.csv");
        assertThat(games).hasSize(2);
        assertThat(games.getFirst().sourceGameId()).isEqualTo("2026-0001");
        assertThat(games.getFirst().homeTeamId()).isEqualTo(1L);
        assertThat(games.getFirst().awayTeamId()).isEqualTo(8L);
        assertThat(games.getFirst().stadiumId()).isEqualTo(1L);
        assertThat(games.getFirst().date()).isEqualTo(LocalDate.of(2026, 3, 28));
        assertThat(games.getFirst().time()).isEqualTo(LocalTime.of(18, 30));
        assertThat(games.getFirst().homeScore()).isNull();
        assertThat(games.get(1).homeScore()).isEqualTo(5);
        assertThat(games.get(1).awayScore()).isEqualTo(3);
    }

    @Test
    @DisplayName("월별 CSV가 없으면 빈 목록을 반환한다")
    void fetchMonthlySchedule_withoutCsv_returnsEmptyList() {
        var games = adapter.fetchMonthlySchedule(YearMonth.of(2026, 3));

        assertThat(games).isEmpty();
    }

    private static class FakeObjectStorage implements ObjectStorage {
        private byte[] bytes;
        private String requestedKey;

        @Override
        public StoredObject upload(ObjectUploadCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String objectKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<byte[]> download(String objectKey) {
            requestedKey = objectKey;
            return Optional.ofNullable(bytes);
        }

        @Override
        public String getUrl(String objectKey) {
            return "https://cdn.example.com/" + objectKey;
        }

        @Override
        public String extractKey(String url) {
            return null;
        }
    }
}
