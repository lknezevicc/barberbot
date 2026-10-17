package hr.lknezevic.barberbot.admin.controller;

import hr.lknezevic.barberbot.admin.audit.Auditable;
import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.entity.ServiceEntity;
import hr.lknezevic.barberbot.admin.entity.Tenant;
import hr.lknezevic.barberbot.admin.repository.ServiceRepository;
import hr.lknezevic.barberbot.admin.repository.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;

    @GetMapping
    public List<ServiceEntity> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return serviceRepository.findByTenantId(principal.tenantId());
    }

    @GetMapping("/active")
    public List<ServiceEntity> listActive(@AuthenticationPrincipal JwtPrincipal principal) {
        return serviceRepository.findByTenantIdAndActiveTrue(principal.tenantId());
    }

    @PostMapping
    @Auditable(action = "CREATE", entityType = "SERVICE")
    public ResponseEntity<?> create(@Valid @RequestBody ServiceRequest request,
                                    @AuthenticationPrincipal JwtPrincipal principal) {
        if (serviceRepository.existsByTenantIdAndNameIgnoreCase(principal.tenantId(), request.name())) {
            return ResponseEntity.status(409).body(Map.of("error", "Service with that name already exists"));
        }

        Tenant tenant = tenantRepository.getReferenceById(principal.tenantId());
        ServiceEntity service = ServiceEntity.builder()
                .tenant(tenant)
                .name(request.name())
                .durationMinutes(request.durationMinutes())
                .price(request.price())
                .description(request.description())
                .active(true)
                .build();
        return ResponseEntity.ok(serviceRepository.save(service));
    }

    @PutMapping("/{id}")
    @Auditable(action = "UPDATE", entityType = "SERVICE")
    public ResponseEntity<ServiceEntity> update(@PathVariable UUID id,
                                                @Valid @RequestBody ServiceRequest request,
                                                @AuthenticationPrincipal JwtPrincipal principal) {
        return serviceRepository.findById(id)
                .filter(s -> s.getTenant().getId().equals(principal.tenantId()))
                .map(service -> {
                    service.setName(request.name());
                    service.setDurationMinutes(request.durationMinutes());
                    service.setPrice(request.price());
                    service.setDescription(request.description());
                    return ResponseEntity.ok(serviceRepository.save(service));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/activate")
    @Auditable(action = "ACTIVATE", entityType = "SERVICE")
    public ResponseEntity<ServiceEntity> activate(@PathVariable UUID id,
                                                  @AuthenticationPrincipal JwtPrincipal principal) {
        return serviceRepository.findById(id)
                .filter(s -> s.getTenant().getId().equals(principal.tenantId()))
                .map(service -> {
                    service.setActive(true);
                    return ResponseEntity.ok(serviceRepository.save(service));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/deactivate")
    @Auditable(action = "DEACTIVATE", entityType = "SERVICE")
    public ResponseEntity<ServiceEntity> deactivate(@PathVariable UUID id,
                                                    @AuthenticationPrincipal JwtPrincipal principal) {
        return serviceRepository.findById(id)
                .filter(s -> s.getTenant().getId().equals(principal.tenantId()))
                .map(service -> {
                    service.setActive(false);
                    return ResponseEntity.ok(serviceRepository.save(service));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record ServiceRequest(
            @NotBlank String name,
            @Min(5) int durationMinutes,
            BigDecimal price,
            String description
    ) {}
}
