package com.sportsmate.server.infrastructure.adapter.out.persistence.content;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerJpaRepository extends JpaRepository<BannerEntity, Long> {

    @Query("""
            select b from BannerEntity b
            where b.active = true
              and (b.startsAt is null or b.startsAt <= :now)
              and (b.endsAt is null or b.endsAt > :now)
            order by b.displayOrder asc, b.id asc
            """)
    List<BannerEntity> findActive(@Param("now") LocalDateTime now);
}
