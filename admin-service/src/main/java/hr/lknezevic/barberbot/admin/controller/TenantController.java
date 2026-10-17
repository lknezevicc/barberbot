package hr.lknezevic.barberbot.admin.controller;

import hr.lknezevic.barberbot.admin.audit.Auditable;
import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.entity.Tenant;
import hr.lknezevic.barberbot.admin.repository.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public List<Tenant> listAll() {
        return tenantRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getById(@PathVariable UUID id, @AuthenticationPrincipal JwtPrincipal principal) {
        if (!"SUPERADMIN".equals(principal.role()) && !principal.tenantId().equals(id)) {
            return ResponseEntity.status(403).build();
        }
        return tenantRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Auditable(action = "CREATE", entityType = "TENANT")
    public ResponseEntity<Tenant> create(@Valid @RequestBody TenantRequest request) {
        if (tenantRepository.existsByName(request.name())) {
            return ResponseEntity.badRequest().build();
        }
        Tenant tenant = Tenant.builder()
                .name(request.name())
                .botToken(request.botToken())
                .botUsername(request.botUsername())
                .secretToken(request.secretToken())
                .contactEmail(request.contactEmail())
                .address(request.address())
                .active(true)
                .build();
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    @PutMapping("/{id}")
    @Auditable(action = "UPDATE", entityType = "TENANT")
    public ResponseEntity<Tenant> update(@PathVariable UUID id,
                                         @Valid @RequestBody TenantRequest request,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        boolean isSuperAdmin = "SUPERADMIN".equals(principal.role());
        boolean isOwnTenant = principal.tenantId().equals(id);

        if (!isSuperAdmin && !isOwnTenant) {
            return ResponseEntity.status(403).build();
        }

        return tenantRepository.findById(id)
                .map(tenant -> {
                    tenant.setName(request.name());
                    tenant.setBotToken(request.botToken());
                    tenant.setBotUsername(request.botUsername());
                    tenant.setSecretToken(request.secretToken());
                    tenant.setContactEmail(request.contactEmail());
                    tenant.setAddress(request.address());
                    return ResponseEntity.ok(tenantRepository.save(tenant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Auditable(action = "DELETE", entityType = "TENANT")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        return tenantRepository.findById(id)
                .map(tenant -> {
                    tenant.setActive(false);
                    tenantRepository.save(tenant);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record TenantRequest(
            @NotBlank String name,
            @NotBlank String botToken,
            String botUsername,
            String secretToken,
            String contactEmail,
            String address
    ) {}
}
