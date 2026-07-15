package com.sportsmate.server.domain.member.port.out;

import com.sportsmate.server.domain.member.enums.WithdrawalReason;
import java.time.LocalDateTime;

public interface MemberWithdrawalLogPort {

    void save(WithdrawalLog log);

    record WithdrawalLog(
            Long memberId,
            String phone,
            String nickname,
            WithdrawalReason reason,
            String reasonDetail,
            LocalDateTime withdrawnAt) {}
}
