package com.sportsmate.server.infrastructure.adapter.out.persistence.content;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqJpaRepository extends JpaRepository<FaqEntity, Long> {

    List<FaqEntity> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
}
