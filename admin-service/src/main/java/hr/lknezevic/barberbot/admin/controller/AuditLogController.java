package hr.lknezevic.barberbot.admin.controller;

import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.entity.AuditLog;
import hr.lknezevic.barberbot.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public Page<AuditLog> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal JwtPrincipal principal) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        String entityTypeParam = (entityType != null && !entityType.isBlank()) ? entityType : null;
        String actionParam = (action != null && !action.isBlank()) ? action : null;
        Instant startInstant = startDate != null ? startDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant endInstant = endDate != null ? endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        if (entityTypeParam != null || actionParam != null || startInstant != null || endInstant != null) {
            return auditLogRepository.findFiltered(
                    principal.tenantId(), entityTypeParam, actionParam, startInstant, endInstant, pageRequest);
        }

        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(principal.tenantId(), pageRequest);
    }
}
