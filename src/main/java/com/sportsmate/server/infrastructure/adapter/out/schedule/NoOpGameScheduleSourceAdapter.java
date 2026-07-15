package com.sportsmate.server.infrastructure.adapter.out.schedule;

import com.sportsmate.server.domain.game.port.out.GameScheduleSourcePort;
import java.time.YearMonth;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class NoOpGameScheduleSourceAdapter implements GameScheduleSourcePort {

    @Override
    public List<SourceGame> fetchMonthlySchedule(YearMonth month) {
        return List.of();
    }
}
