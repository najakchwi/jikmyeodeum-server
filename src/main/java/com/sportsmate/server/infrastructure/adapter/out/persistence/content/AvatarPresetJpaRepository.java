package com.sportsmate.server.infrastructure.adapter.out.persistence.content;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvatarPresetJpaRepository extends JpaRepository<AvatarPresetEntity, Long> {

    List<AvatarPresetEntity> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
}
