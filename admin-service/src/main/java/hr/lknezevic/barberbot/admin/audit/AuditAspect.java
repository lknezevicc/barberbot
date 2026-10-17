package hr.lknezevic.barberbot.admin.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.entity.AuditLog;
import hr.lknezevic.barberbot.admin.entity.Tenant;
import hr.lknezevic.barberbot.admin.repository.AuditLogRepository;
import hr.lknezevic.barberbot.admin.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        Object result = pjp.proceed();

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
                return result;
            }

            UUID entityId = extractEntityId(result);
            String newValue = extractValue(result);

            Tenant tenant = tenantRepository.getReferenceById(principal.tenantId());

            AuditLog logEntry = AuditLog.builder()
                    .tenant(tenant)
                    .actorId(principal.userId())
                    .actorName(principal.role())
                    .action(auditable.action())
                    .entityType(auditable.entityType())
                    .entityId(entityId)
                    .newValue(newValue)
                    .build();

            auditLogRepository.save(logEntry);
            log.debug("Audit logged: action={}, entityType={}, entityId={}",
                    auditable.action(), auditable.entityType(), entityId);
        } catch (Exception e) {
            log.warn("Failed to write audit log for action={}: {}", auditable.action(), e.getMessage());
        }

        return result;
    }

    private UUID extractEntityId(Object result) {
        if (result instanceof ResponseEntity<?> re && re.getBody() != null) {
            Object body = re.getBody();
            try {
                var idField = body.getClass().getMethod("getId");
                Object id = idField.invoke(body);
                if (id instanceof UUID uuid) return uuid;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractValue(Object result) {
        try {
            if (result instanceof ResponseEntity<?> re && re.getBody() != null) {
                return objectMapper.writeValueAsString(re.getBody());
            }
        } catch (Exception ignored) {}
        return null;
    }
}
