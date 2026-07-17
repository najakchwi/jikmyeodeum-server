package com.sportsmate.server.infrastructure.adapter.out.persistence.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.matching.MatchReason;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.infrastructure.adapter.out.persistence.game.GameJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.match.MatchEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.match.MatchJpaRepository;
import com.sportsmate.server.infrastructure.adapter.out.persistence.match.MatchParticipantEntity;
import com.sportsmate.server.infrastructure.adapter.out.persistence.match.MatchParticipantId;
import com.sportsmate.server.infrastructure.adapter.out.persistence.match.MatchParticipantJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
public class ApplicationPersistenceAdapter implements ApplicationOutPort {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<MatchReason>> MATCH_REASON_LIST_TYPE = new TypeReference<>() {};

    private final ApplicationJpaRepository repository;
    private final GameJpaRepository gameRepository;
    private final MatchJpaRepository matchRepository;
    private final MatchParticipantJpaRepository matchParticipantRepository;

    public ApplicationPersistenceAdapter(ApplicationJpaRepository repository,
            GameJpaRepository gameRepository, MatchJpaRepository matchRepository,
            MatchParticipantJpaRepository matchParticipantRepository) {
        this.repository = repository;
        this.gameRepository = gameRepository;
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
    }

    @Override
    public Application save(Application application) {
        return toDomain(repository.save(toEntity(application)));
    }

    @Override
    public Application saveAndFlush(Application application) {
        return toDomain(repository.saveAndFlush(toEntity(application)));
    }

    @Override
    public String createMatch(Application application, Application opponent) {
        LocalDateTime now = LocalDateTime.now();
        MatchEntity match = matchRepository.save(MatchEntity.builder()
                .gameId(Long.parseLong(application.getGameId()))
                .status("chatting")
                .matchedAt(now)
                .expiresAt(application.getExpiresAt())
                .createdAt(now)
                .build());
        saveParticipant(match.getId(), application);
        saveParticipant(match.getId(), opponent);
        return String.valueOf(match.getId());
    }

    @Override
    public String createSoloMatch(Application application) {
        LocalDateTime now = LocalDateTime.now();
        MatchEntity match = matchRepository.save(MatchEntity.builder()
                .gameId(Long.parseLong(application.getGameId()))
                .status("chatting")
                .matchedAt(now)
                .expiresAt(application.getExpiresAt())
                .createdAt(now)
                .build());
        saveParticipant(match.getId(), application);
        return String.valueOf(match.getId());
    }

    @Override
    public void addParticipant(String chatId, Application application) {
        saveParticipant(Long.parseLong(chatId), application);
    }

    @Override public Optional<Application> findByIdAndMemberId(String id, Long memberId) {
        return repository.findByIdAndMemberId(Long.parseLong(id), memberId).map(this::toDomain);
    }
    @Override public Optional<Application> findByMemberIdAndGameId(Long memberId, String gameId) {
        // cancelled를 제외한 활성 신청(최대 1건)만 조회한다. 취소 후 재신청 시 행이 여러 개가 되어도
        // 쿼리 레벨에서 단일 결과를 보장해 NonUniqueResultException을 막는다.
        return repository.findFirstByMemberIdAndGameIdAndStatusNotOrderByAppliedAtDesc(
                memberId, Long.parseLong(gameId), "cancelled").map(this::toDomain);
    }
    @Override public Optional<Application> findByChatIdAndMemberId(String chatId, Long memberId) {
        return repository.findByMatchIdAndMemberId(Long.parseLong(chatId), memberId).map(this::toDomain);
    }
    @Override public List<Application> findByMemberId(Long memberId) {
        return repository.findByMemberIdOrderByAppliedAtDesc(memberId).stream()
                .map(this::toDomain).toList();
    }
    @Override public List<Application> findWaitingByGameId(String gameId) {
        return repository.findByGameIdAndStatusOrderByAppliedAtAsc(Long.parseLong(gameId), "waiting").stream()
                .map(this::toDomain).toList();
    }
    @Override public List<String> findGameIdsWithWaitingApplications() {
        return repository.findDistinctGameIdByStatus("waiting").stream()
                .map(String::valueOf).toList();
    }
    @Override public boolean existsActiveByMemberIdAndGameId(Long memberId, String gameId) {
        return repository.existsByMemberIdAndGameIdAndStatusNot(memberId, Long.parseLong(gameId), "cancelled");
    }
    @Override public boolean existsActiveByMemberIdAndDate(Long memberId, LocalDate date) {
        return repository.existsByMemberIdAndGameDateAndStatusNot(memberId, date, "cancelled");
    }

    @Override
    public List<LocalDate> findAppliedDates(Long memberId, int year, int month) {
        YearMonth target = YearMonth.of(year, month);
        return findByMemberId(memberId).stream()
                .map(Application::getGameId)
                .map(gameId -> gameRepository.findById(Long.parseLong(gameId)))
                .flatMap(Optional::stream)
                .map(entity -> entity.getDate())
                .filter(date -> YearMonth.from(date).equals(target))
                .distinct().sorted().toList();
    }

    @Override
    public long countChattingCancellationsSince(Long memberId, LocalDateTime since) {
        return repository.countByMemberIdAndStatusAndMatchIdIsNotNullAndCancelledAtGreaterThanEqual(
                memberId, "cancelled", since);
    }

    private void saveParticipant(Long matchId, Application application) {
        matchParticipantRepository.save(MatchParticipantEntity.builder()
                .id(MatchParticipantId.of(matchId, application.getMemberId()))
                .applicationId(Long.parseLong(application.getId()))
                .response(application.getResponse())
                .joinedAt(LocalDateTime.now())
                .build());
    }

    private ApplicationEntity toEntity(Application application) {
        Long id = application.getId() == null ? null : Long.parseLong(application.getId());
        LocalDate gameDate = resolveGameDate(application, id);
        return ApplicationEntity.builder()
                .id(id)
                .memberId(application.getMemberId())
                .gameId(Long.parseLong(application.getGameId()))
                .gameDate(gameDate)
                .matchId(application.getChatId() == null ? null : Long.parseLong(application.getChatId()))
                .matchedMemberId(application.getMatchedMemberId())
                .matchedAt(application.getMatchedAt())
                .expiresAt(application.getExpiresAt())
                .response(application.getResponse())
                .partySize(1)
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .cancelledAt(application.getCancelledAt())
                .matchScore(application.getMatchScore())
                .matchReasons(writeMatchReasons(application.getMatchReasons()))
                .version(application.getVersion())
                .rejectedMemberIds(application.getRejectedMemberIds())
                .build();
    }

    private Application toDomain(ApplicationEntity entity) {
        return Application.reconstitute(
                String.valueOf(entity.getId()),
                entity.getMemberId(),
                String.valueOf(entity.getGameId()),
                entity.getGameDate(),
                entity.getStatus(),
                entity.getAppliedAt(),
                entity.getMatchedMemberId(),
                entity.getMatchId() == null ? null : String.valueOf(entity.getMatchId()),
                entity.getMatchedAt(),
                entity.getExpiresAt(),
                entity.getResponse(),
                entity.getCancelledAt(),
                entity.getMatchScore(),
                readMatchReasons(entity.getMatchReasons()),
                entity.getRejectedMemberIds(),
                entity.getVersion());
    }

    private String writeMatchReasons(List<MatchReason> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(reasons);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize application match reasons", exception);
        }
    }

    private List<MatchReason> readMatchReasons(String reasonsJson) {
        if (reasonsJson == null || reasonsJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(reasonsJson, MATCH_REASON_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize application match reasons", exception);
        }
    }

    private LocalDate resolveGameDate(Application application, Long id) {
        if (application.getGameDate() != null) {
            return application.getGameDate();
        }
        if (id != null) {
            return repository.findById(id)
                    .map(ApplicationEntity::getGameDate)
                    .orElseGet(() -> findGameDate(application.getGameId()));
        }
        return findGameDate(application.getGameId());
    }

    private LocalDate findGameDate(String gameId) {
        return gameRepository.findById(Long.parseLong(gameId))
                .orElseThrow(() -> new IllegalStateException("Game not found: " + gameId))
                .getDate();
    }
}
