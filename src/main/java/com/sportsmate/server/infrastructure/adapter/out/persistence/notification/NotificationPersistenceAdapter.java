package com.sportsmate.server.infrastructure.adapter.out.persistence.notification;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.domain.notification.port.out.NotificationOutPort;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
public class NotificationPersistenceAdapter implements NotificationOutPort {
    private final NotificationJpaRepository repository;
    private final NotificationSettingsJpaRepository settingsRepository;

    public NotificationPersistenceAdapter(NotificationJpaRepository repository,
            NotificationSettingsJpaRepository settingsRepository) {
        this.repository = repository;
        this.settingsRepository = settingsRepository;
    }
    @Override
    public List<NotificationData> findByMemberId(Long memberId, String cursor, int size) {
        List<NotificationEntity> all = repository.findByMemberIdOrderByCreatedAtDesc(memberId);
        int start = 0;
        if (cursor != null) {
            for (int i = 0; i < all.size(); i++) {
                if (String.valueOf(all.get(i).getId()).equals(cursor)) { start = i + 1; break; }
            }
        }
        return all.stream().skip(start).limit(size).map(this::toData).toList();
    }
    @Override public long countUnread(Long memberId) { return repository.countByMemberIdAndReadFalse(memberId); }
    @Override public Optional<NotificationData> findByIdAndMemberId(String id, Long memberId) {
        return repository.findByIdAndMemberId(Long.parseLong(id), memberId).map(this::toData);
    }
    @Override public void markRead(String id) {
        repository.findById(Long.parseLong(id))
                .ifPresent(entity -> repository.save(readEntity(entity)));
    }
    @Override public void markAllRead(Long memberId) {
        List<NotificationEntity> entities = repository.findByMemberIdOrderByCreatedAtDesc(memberId);
        repository.saveAll(entities.stream().map(this::readEntity).toList());
    }
    @Override public SettingsData getOrCreateSettings(Long memberId) {
        return toData(settingsRepository.findById(memberId)
                .orElseGet(() -> settingsRepository.save(defaultSettings(memberId))));
    }
    @Override public SettingsData saveSettings(SettingsData settings) {
        NotificationSettingsEntity entity = NotificationSettingsEntity.builder()
                .memberId(settings.memberId())
                .matchRequest(settings.matchRequest())
                .matchSchedule(settings.matchSchedule())
                .chat(settings.chat())
                .review(settings.review())
                .marketing(settings.marketing())
                .build();
        return toData(settingsRepository.save(entity));
    }
    @Override
    public void create(Long memberId, String type, String title, String body, String targetKind,
            String applicationId, String chatId) {
        repository.save(NotificationEntity.builder()
                .memberId(memberId)
                .applicationId(applicationId == null ? null : Long.parseLong(applicationId))
                .matchId(chatId == null ? null : Long.parseLong(chatId))
                .type(type)
                .title(title)
                .body(body)
                .targetKind(targetKind)
                .read(false)
                .createdAt(java.time.LocalDateTime.now())
                .build());
    }
    private NotificationData toData(NotificationEntity entity) {
        return new NotificationData(String.valueOf(entity.getId()), entity.getMemberId(), entity.getType(),
                entity.getTitle(), entity.getBody(), entity.getCreatedAt(), entity.getRead(),
                entity.getTargetKind(),
                entity.getApplicationId() == null ? null : String.valueOf(entity.getApplicationId()),
                entity.getMatchId() == null ? null : String.valueOf(entity.getMatchId()));
    }
    private SettingsData toData(NotificationSettingsEntity entity) {
        return new SettingsData(entity.getMemberId(), entity.getMatchRequest(),
                entity.getMatchSchedule(), entity.getChat(), entity.getReview(), entity.getMarketing());
    }

    private NotificationSettingsEntity defaultSettings(Long memberId) {
        return NotificationSettingsEntity.builder()
                .memberId(memberId)
                .matchRequest(true)
                .matchSchedule(true)
                .chat(true)
                .review(true)
                .marketing(false)
                .build();
    }

    private NotificationEntity readEntity(NotificationEntity entity) {
        return NotificationEntity.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .applicationId(entity.getApplicationId())
                .matchId(entity.getMatchId())
                .type(entity.getType())
                .title(entity.getTitle())
                .body(entity.getBody())
                .targetKind(entity.getTargetKind())
                .read(true)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
