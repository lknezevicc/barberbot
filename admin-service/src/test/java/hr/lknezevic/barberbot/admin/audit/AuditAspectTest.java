package hr.lknezevic.barberbot.admin.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.entity.AuditLog;
import hr.lknezevic.barberbot.admin.entity.Tenant;
import hr.lknezevic.barberbot.admin.repository.AuditLogRepository;
import hr.lknezevic.barberbot.admin.repository.TenantRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private TenantRepository tenantRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuditAspect auditAspect;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private Auditable mockAuditable(String action, String entityType) {
        Auditable auditable = mock(Auditable.class);
        lenient().when(auditable.action()).thenReturn(action);
        lenient().when(auditable.entityType()).thenReturn(entityType);
        return auditable;
    }

    @Test
    void audit_capturesActionAndPersists() throws Throwable {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, tenantId, "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        Tenant tenant = Tenant.builder().id(tenantId).name("Salon").build();
        when(tenantRepository.getReferenceById(tenantId)).thenReturn(tenant);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("some result");

        Auditable auditable = mockAuditable("CREATE", "EMPLOYEE");
        auditAspect.audit(pjp, auditable);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntityType()).isEqualTo("EMPLOYEE");
        assertThat(saved.getActorId()).isEqualTo(userId);
        assertThat(saved.getTenant()).isEqualTo(tenant);
    }

    @Test
    void audit_noAuthentication_skipsLog() throws Throwable {
        SecurityContextHolder.clearContext();

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("result");

        Auditable auditable = mockAuditable("UPDATE", "SERVICE");
        Object result = auditAspect.audit(pjp, auditable);

        assertThat(result).isEqualTo("result");
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void audit_exceptionInLogging_doesNotPropagateAndReturnsResult() throws Throwable {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, tenantId, "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        when(tenantRepository.getReferenceById(tenantId)).thenThrow(new RuntimeException("DB down"));

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("result");

        Auditable auditable = mockAuditable("DELETE", "BOOKING");
        Object result = auditAspect.audit(pjp, auditable);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void audit_proceedIsAlwaysCalled() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("ok");

        Auditable auditable = mockAuditable("READ", "TENANT");
        auditAspect.audit(pjp, auditable);

        verify(pjp).proceed();
    }
}
