package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.persistence.BaseTimeEntity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "member_watch_styles")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWatchStyleEntity extends BaseTimeEntity implements Persistable<MemberWatchStyleId> {

    @EmbeddedId
    private MemberWatchStyleId id;

    @Override
    public boolean isNew() {
        // 이 테이블은 항상 deleteAllByMemberId 후 재삽입되는 조인 테이블이라
        // assigned composite id만으로는 Spring Data가 신규 여부를 판단할 수 없다.
        // BaseTimeEntity가 updated_at/updated_by를 추가하면서 merge()가 (존재하지
        // 않는 행에 대한) UPDATE를 시도해 ObjectOptimisticLockingFailureException이
        // 나던 문제 — 항상 insert(persist)로 처리되도록 고정한다.
        return true;
    }
}
