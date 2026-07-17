package com.sportsmate.server.infrastructure.adapter.out.persistence.game;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.domain.game.Game;
import com.sportsmate.server.domain.game.event.GameRescheduledEvent;
import com.sportsmate.server.domain.game.port.out.GameOutPort;
import com.sportsmate.server.infrastructure.adapter.out.persistence.application.ApplicationJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
public class GamePersistenceAdapter implements GameOutPort {
    private final GameJpaRepository repository;
    private final ApplicationJpaRepository applicationRepository;
    private final TeamJpaRepository teamRepository;
    private final StadiumJpaRepository stadiumRepository;
    private final ObjectStorage objectStorage;

    public GamePersistenceAdapter(GameJpaRepository repository,
            ApplicationJpaRepository applicationRepository,
            TeamJpaRepository teamRepository,
            StadiumJpaRepository stadiumRepository,
            ObjectStorage objectStorage) {
        this.repository = repository;
        this.applicationRepository = applicationRepository;
        this.teamRepository = teamRepository;
        this.stadiumRepository = stadiumRepository;
        this.objectStorage = objectStorage;
    }

    private NameLookup loadLookup() {
        List<TeamEntity> teams = teamRepository.findAll();
        Map<Long, String> teamNames = teams.stream()
                .collect(Collectors.toMap(
                        TeamEntity::getId,
                        e -> e.getShortName() != null ? e.getShortName() : e.getName()));
        Map<Long, String> teamEmblemUrls = teams.stream()
                .filter(e -> e.getEmblemImageKey() != null)
                .collect(Collectors.toMap(
                        TeamEntity::getId,
                        e -> objectStorage.getUrl(e.getEmblemImageKey())));
        Map<Long, String> stadiumNames = stadiumRepository.findAll().stream()
                .collect(Collectors.toMap(StadiumEntity::getId, StadiumEntity::getName));
        return new NameLookup(teamNames, teamEmblemUrls, stadiumNames);
    }

    @Override
    public List<Game> findBetween(LocalDate startDate, LocalDate endDate) {
        var lookup = loadLookup();
        return repository.findByDateBetweenOrderByDateAscTimeAsc(startDate, endDate).stream()
                .map(entity -> toDomain(entity, lookup)).toList();
    }

    @Override
    public Optional<SeasonRange> findSeasonRange() {
        return repository.findFirstByOrderByDateAsc()
                .flatMap(first -> repository.findFirstByOrderByDateDesc()
                        .map(last -> new SeasonRange(first.getDate(), last.getDate())));
    }

    @Override
    public List<Game> findByDate(LocalDate date) {
        var lookup = loadLookup();
        return repository.findByDateOrderByTimeAsc(date).stream().map(entity -> toDomain(entity, lookup)).toList();
    }
    @Override
    public Optional<Game> findById(String id) {
        var lookup = loadLookup();
        return repository.findById(parseId(id)).map(entity -> toDomain(entity, id, lookup));
    }
    @Override
    public long countApplications(String gameId) {
        return applicationRepository.countByGameIdAndStatusNot(parseId(gameId), "cancelled");
    }
    @Override
    public long countWaitingApplications(String gameId) {
        return applicationRepository.countByGameIdAndStatus(parseId(gameId), "waiting");
    }

    @Override
    @Transactional
    public UpsertResult upsertAll(List<GameSyncCommand> commands) {
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        int updated = 0;
        List<GameRescheduledEvent> rescheduledEvents = new ArrayList<>();

        for (var command : commands) {
            var existing = repository.findByKboGameId(command.kboGameId());
            if (existing.isEmpty()) {
                repository.save(toEntity(command, now));
                created++;
                continue;
            }

            var entity = existing.get();
            boolean scheduleChanged = !Objects.equals(entity.getDate(), command.date())
                    || !Objects.equals(entity.getTime(), command.time());
            LocalDate previousDate = entity.getDate();
            var previousTime = entity.getTime();
            boolean changed = entity.updateFrom(toSyncValues(command), now);

            if (changed) {
                updated++;
                if (scheduleChanged && countApplications(String.valueOf(entity.getId())) > 0) {
                    rescheduledEvents.add(new GameRescheduledEvent(
                            String.valueOf(entity.getId()),
                            previousDate,
                            previousTime,
                            command.date(),
                            command.time()
                    ));
                }
            }
        }

        return new UpsertResult(created, updated, rescheduledEvents);
    }

    @Override
    @Transactional
    public int cancelMissingSyncedGames(YearMonth month, List<String> fetchedKboGameIds) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        LocalDateTime now = LocalDateTime.now();
        int cancelled = 0;

        for (var entity : repository.findByDateBetweenAndKboGameIdIsNotNull(start, end)) {
            if (!fetchedKboGameIds.contains(entity.getKboGameId()) && !"CANCELLED".equals(entity.getStatus())) {
                entity.cancel(now);
                cancelled++;
            }
        }

        return cancelled;
    }

    private Game toDomain(GameEntity entity, NameLookup lookup) {
        return toDomain(entity, String.valueOf(entity.getId()), lookup);
    }

    private Game toDomain(GameEntity entity, String id, NameLookup lookup) {
        return new Game(
                id,
                lookup.teamNames().getOrDefault(entity.getHomeTeamId(), String.valueOf(entity.getHomeTeamId())),
                lookup.teamNames().getOrDefault(entity.getAwayTeamId(), String.valueOf(entity.getAwayTeamId())),
                lookup.stadiumNames().getOrDefault(entity.getStadiumId(), String.valueOf(entity.getStadiumId())),
                entity.getDate(),
                entity.getTime(),
                entity.getDeadline(),
                entity.getHomeScore(),
                entity.getAwayScore(),
                lookup.teamEmblemUrls().get(entity.getHomeTeamId()),
                lookup.teamEmblemUrls().get(entity.getAwayTeamId()),
                entity.getHomeTeamId(),
                entity.getAwayTeamId(),
                entity.getLeagueId());
    }

    private record NameLookup(
            Map<Long, String> teamNames,
            Map<Long, String> teamEmblemUrls,
            Map<Long, String> stadiumNames
    ) {
    }

    private Long parseId(String id) {
        return Long.parseLong(id.replaceFirst("^[^0-9]+", ""));
    }

    private GameEntity toEntity(GameSyncCommand command, LocalDateTime syncedAt) {
        return GameEntity.builder()
                .leagueId(command.leagueId())
                .homeTeamId(command.homeTeamId())
                .awayTeamId(command.awayTeamId())
                .stadiumId(command.stadiumId())
                .date(command.date())
                .time(command.time())
                .deadline(command.date().minusDays(1))
                .status(command.status())
                .homeScore(command.homeScore())
                .awayScore(command.awayScore())
                .kboGameId(command.kboGameId())
                .lastSyncedAt(syncedAt)
                .build();
    }

    private GameEntity.GameSyncValues toSyncValues(GameSyncCommand command) {
        return new GameEntity.GameSyncValues(
                command.leagueId(),
                command.homeTeamId(),
                command.awayTeamId(),
                command.stadiumId(),
                command.date(),
                command.time(),
                command.date().minusDays(1),
                command.status(),
                command.homeScore(),
                command.awayScore()
        );
    }
}
