package com.sportsmate.server.infrastructure.adapter.out.persistence.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingsJpaRepository
        extends JpaRepository<NotificationSettingsEntity, Long> {
}
