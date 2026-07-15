package com.sportsmate.server.domain.report.service;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditEvent;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.common.port.out.audit.AuditResult;
import com.sportsmate.server.domain.application.Application;
import com.sportsmate.server.domain.application.port.out.ApplicationOutPort;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.exception.MemberErrorCode;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.report.exception.ReportErrorCode;
import com.sportsmate.server.domain.report.port.in.ReportUseCase;
import com.sportsmate.server.domain.report.port.out.ReportOutPort;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService implements ReportUseCase {
    private final ReportOutPort reportOutPort;
    private final ApplicationOutPort applicationOutPort;
    private final MemberOutPort memberOutPort;
    private final AuditLogPort auditLogPort;

    public ReportService(ReportOutPort reportOutPort, ApplicationOutPort applicationOutPort,
            MemberOutPort memberOutPort, AuditLogPort auditLogPort) {
        this.reportOutPort = reportOutPort;
        this.applicationOutPort = applicationOutPort;
        this.memberOutPort = memberOutPort;
        this.auditLogPort = auditLogPort;
    }

    @Override
    @Transactional
    public ReportResult report(Long reporterId, Long targetUserId, String applicationId,
            String chatId, String reason, String detail) {
        if (reporterId.equals(targetUserId)) {
            throw new BusinessException(ReportErrorCode.CANNOT_REPORT_SELF);
        }
        Application application = applicationOutPort.findByIdAndMemberId(applicationId, reporterId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
        if (!targetUserId.equals(application.getMatchedMemberId())
                || !chatId.equals(application.getChatId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (reportOutPort.existsByReporterIdAndTargetUserIdAndChatId(reporterId, targetUserId, chatId)) {
            throw new BusinessException(ReportErrorCode.ALREADY_REPORTED);
        }
        String id = reportOutPort.save(reporterId, targetUserId, applicationId, chatId, reason, detail);
        auditLogPort.record(AuditEvent.of(
                AuditCategory.REPORT, "REPORT_RECEIVED", "MEMBER", reporterId.toString(),
                "MEMBER", targetUserId.toString(), AuditResult.SUCCESS,
                Map.of("reportId", id, "reason", reason, "applicationId", applicationId)));

        Member target = memberOutPort.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        target.addTrustScore(-15);
        memberOutPort.save(target);
        auditLogPort.record(AuditEvent.of(
                AuditCategory.TRUST_SCORE, "TRUST_SCORE_DEDUCT", "SYSTEM", null,
                "MEMBER", targetUserId.toString(), AuditResult.SUCCESS,
                Map.of("delta", -15, "reason", "REPORT_RECEIVED", "reportId", id)));
        return new ReportResult(id, "received");
    }
}
