package com.sportsmate.server.infrastructure.adapter.in.scheduler;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import com.sportsmate.server.domain.game.port.in.SyncSeasonScheduleUseCase;
import java.time.YearMonth;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class GameScheduleSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(GameScheduleSyncScheduler.class);

    private final SyncSeasonScheduleUseCase syncSeasonScheduleUseCase;
    private final OpsAlertPort opsAlertPort;
    private final JobHeartbeat jobHeartbeat;

    public GameScheduleSyncScheduler(
            SyncSeasonScheduleUseCase syncSeasonScheduleUseCase,
            OpsAlertPort opsAlertPort,
            JobHeartbeat jobHeartbeat) {
        this.syncSeasonScheduleUseCase = syncSeasonScheduleUseCase;
        this.opsAlertPort = opsAlertPort;
        this.jobHeartbeat = jobHeartbeat;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void syncCurrentAndNextMonth() {
        opsAlertPort.notify(AlertSeverity.INFO, AlertMessage.of(
                "경기일정 동기화 시작",
                "현재월과 다음월 KBO 경기일정 동기화를 시작했습니다.",
                "job:game-sync:start:" + YearMonth.now()));
        YearMonth currentMonth = YearMonth.now();
        try {
            var current = sync(currentMonth);
            var next = sync(currentMonth.plusMonths(1));
            jobHeartbeat.markSuccess("game-schedule-sync");
            if (current.created() + current.updated() + current.cancelled()
                    + next.created() + next.updated() + next.cancelled() == 0) {
                opsAlertPort.notify(AlertSeverity.WARNING, new AlertMessage(
                        "경기 데이터 공백",
                        "경기일정 동기화 결과 변경 건수가 0입니다.",
                        Map.of("month", currentMonth.toString(), "nextMonth", currentMonth.plusMonths(1).toString()),
                        "job:game-sync:empty"));
            } else {
                opsAlertPort.resolve("job:game-sync:empty");
            }
            opsAlertPort.notify(AlertSeverity.INFO, AlertMessage.of(
                    "경기일정 동기화 성공",
                    "KBO 경기일정 동기화가 완료되었습니다.",
                    null));
        } catch (RuntimeException exception) {
            jobHeartbeat.markFailure("game-schedule-sync");
            opsAlertPort.notify(AlertSeverity.WARNING, new AlertMessage(
                    "경기일정 동기화 실패",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    Map.of("job", "game-schedule-sync"),
                    "job:game-sync:failure"));
            throw exception;
        }
    }

    private SyncSeasonScheduleUseCase.SyncResult sync(YearMonth month) {
        var result = syncSeasonScheduleUseCase.sync(month);
        log.info(
                "KBO schedule synced. month={}, created={}, updated={}, cancelled={}",
                month,
                result.created(),
                result.updated(),
                result.cancelled()
        );
        return result;
    }
}
