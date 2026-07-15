package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.domain.member.enums.WatchStyle;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWatchStyleId implements Serializable {

    @Column(name = "watch_style", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private WatchStyle watchStyle;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public static MemberWatchStyleId of(WatchStyle watchStyle, Long memberId) {
        return new MemberWatchStyleId(watchStyle, memberId);
    }
}
