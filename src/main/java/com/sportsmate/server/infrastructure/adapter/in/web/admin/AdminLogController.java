package com.sportsmate.server.infrastructure.adapter.in.web.admin;

import com.sportsmate.server.common.port.out.audit.AuditCategory;
import com.sportsmate.server.common.port.out.audit.AuditEvent;
import com.sportsmate.server.common.port.out.audit.AuditLogPort;
import com.sportsmate.server.common.port.out.audit.AuditResult;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import com.sportsmate.server.infrastructure.adapter.out.storage.log.LogType;
import com.sportsmate.server.infrastructure.adapter.out.storage.log.LogUploadResult;
import com.sportsmate.server.infrastructure.adapter.out.storage.log.LogUploadService;
import com.sportsmate.server.infrastructure.security.authorization.RoleAdminAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/logs")
@Tag(name = "Admin", description = "운영자 로그 업로드 API")
@RoleAdminAuth
public class AdminLogController {

    private static final int MAX_UPLOAD_DAYS = 90;

    private final LogUploadService logUploadService;
    private final AuditLogPort auditLogPort;
    private final Path logDir;

    public AdminLogController(LogUploadService logUploadService, AuditLogPort auditLogPort) {
        this.logUploadService = logUploadService;
        this.auditLogPort = auditLogPort;
        this.logDir = Path.of("logs");
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "선택한 로그를 즉시 R2에 업로드",
            description = "지정한 로그 타입과 기간(연-월-일 범위)에 해당하는 로그 파일을 mtime 캐시와 무관하게 강제로 R2에 즉시 업로드한다. "
                    + "주기적인 LogUploadScheduler와는 별개로 동작하며, 스케줄러 주기에 영향을 주지 않는다.")
    public ApiResponse<LogUploadResult> upload(
            @RequestBody UploadLogsRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        Set<LogType> logTypes = parseLogTypes(request.logTypes());
        LocalDate fromDate = parseDate(request.fromDate(), "fromDate");
        LocalDate toDate = parseDate(request.toDate(), "toDate");
        validateRange(fromDate, toDate);

        LogUploadResult result = logUploadService.uploadSelected(logDir, logTypes, fromDate, toDate);

        auditLogPort.record(AuditEvent.of(
                AuditCategory.ADMIN_ACTION, "LOG_UPLOAD_MANUAL", "ADMIN", memberId,
                "LOG_FILE", fromDate + "~" + toDate, AuditResult.SUCCESS,
                Map.of(
                        "logTypes", request.logTypes(),
                        "uploadedFiles", result.uploadedFiles(),
                        "failedFiles", result.failedFiles())));
        return ApiResponse.success(result);
    }

    private Set<LogType> parseLogTypes(List<String> logTypes) {
        if (logTypes == null || logTypes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "logTypes must not be empty");
        }
        var result = EnumSet.noneOf(LogType.class);
        for (String logType : logTypes) {
            try {
                result.add(LogType.valueOf(logType));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid logType: " + logType, e);
            }
        }
        return result;
    }

    private LocalDate parseDate(String date, String fieldName) {
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be yyyy-MM-dd", e);
        }
    }

    private void validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be before or equal to toDate");
        }
        long dayCount = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (dayCount > MAX_UPLOAD_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date range must be " + MAX_UPLOAD_DAYS + " days or less");
        }
    }

    public record UploadLogsRequest(
            @Schema(description = "업로드할 로그 타입 목록", example = "[\"AUDIT\", \"API\"]")
            List<String> logTypes,
            @Schema(description = "업로드 시작 날짜. yyyy-MM-dd 형식", example = "2026-07-01")
            String fromDate,
            @Schema(description = "업로드 종료 날짜. yyyy-MM-dd 형식", example = "2026-07-15")
            String toDate
    ) {
    }
}
