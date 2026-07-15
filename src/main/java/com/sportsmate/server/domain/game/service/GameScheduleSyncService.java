package com.sportsmate.server.domain.game.service;

import com.sportsmate.server.common.port.out.event.EventPublisher;
import com.sportsmate.server.domain.game.port.in.SyncSeasonScheduleUseCase;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.domain.game.port.out.GameScheduleSourcePort;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameScheduleSyncService implements SyncSeasonScheduleUseCase {

    private static final long KBO_LEAGUE_ID = 1L;

    private final GameScheduleSourcePort scheduleSourcePort;
    private final GameOutPort gameOutPort;
    private final EventPublisher eventPublisher;

    public GameScheduleSyncService(
            GameScheduleSourcePort scheduleSourcePort,
            GameOutPort gameOutPort,
            EventPublisher eventPublisher
    ) {
        this.scheduleSourcePort = scheduleSourcePort;
        this.gameOutPort = gameOutPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public SyncResult sync(YearMonth month) {
        var sourceGames = scheduleSourcePort.fetchMonthlySchedule(month);
        var commands = new ArrayList<GameOutPort.GameSyncCommand>();

        for (var sourceGame : sourceGames) {
            commands.add(new GameOutPort.GameSyncCommand(
                    sourceGame.sourceGameId(),
                    KBO_LEAGUE_ID,
                    sourceGame.homeTeamId(),
                    sourceGame.awayTeamId(),
                    sourceGame.stadiumId(),
                    sourceGame.date(),
                    sourceGame.time(),
                    toInternalStatus(sourceGame),
                    sourceGame.homeScore(),
                    sourceGame.awayScore()
            ));
        }

        var upsertResult = gameOutPort.upsertAll(commands);
        upsertResult.rescheduledEvents().forEach(eventPublisher::publish);
        int cancelled = gameOutPort.cancelMissingSyncedGames(
                month,
                sourceGames.stream().map(GameScheduleSourcePort.SourceGame::sourceGameId).toList()
        );

        return new SyncResult(
                upsertResult.created(),
                upsertResult.updated(),
                cancelled
        );
    }

    @Override
    @Transactional
    public SyncRangeResult sync(YearMonth fromMonth, YearMonth toMonth) {
        var monthlyResults = new ArrayList<MonthlySyncResult>();
        int created = 0;
        int updated = 0;
        int cancelled = 0;

        for (YearMonth month = fromMonth; !month.isAfter(toMonth); month = month.plusMonths(1)) {
            SyncResult result = sync(month);
            monthlyResults.add(new MonthlySyncResult(
                    month,
                    result.created(),
                    result.updated(),
                    result.cancelled()
            ));
            created += result.created();
            updated += result.updated();
            cancelled += result.cancelled();
        }

        return new SyncRangeResult(
                fromMonth,
                toMonth,
                List.copyOf(monthlyResults),
                created,
                updated,
                cancelled
        );
    }

    private String toInternalStatus(GameScheduleSourcePort.SourceGame sourceGame) {
        if (sourceGame.homeScore() != null && sourceGame.awayScore() != null) {
            return "FINISHED";
        }
        return "SCHEDULED";
    }
}
