package com.sportsmate.server.infrastructure.adapter.out.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NoOp 경기 일정 소스 어댑터 단위 테스트")
class NoOpGameScheduleSourceAdapterTest {

    @Test
    @DisplayName("실험 환경에서는 빈 경기 일정 목록을 반환한다")
    void fetchMonthlySchedule_returnsEmptyList() {
        NoOpGameScheduleSourceAdapter adapter = new NoOpGameScheduleSourceAdapter();

        assertThat(adapter.fetchMonthlySchedule(YearMonth.of(2026, 7))).isEmpty();
    }
}
