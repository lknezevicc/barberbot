package hr.lknezevic.barberbot.admin.controller;

import hr.lknezevic.barberbot.admin.audit.Auditable;
import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.entity.Employee;
import hr.lknezevic.barberbot.admin.entity.Tenant;
import hr.lknezevic.barberbot.admin.repository.EmployeeRepository;
import hr.lknezevic.barberbot.admin.repository.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final TenantRepository tenantRepository;

    @GetMapping
    public List<Employee> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return employeeRepository.findByTenantId(principal.tenantId());
    }

    @GetMapping("/active")
    public List<Employee> listActive(@AuthenticationPrincipal JwtPrincipal principal) {
        return employeeRepository.findByTenantIdAndActiveTrue(principal.tenantId());
    }

    @PostMapping
    @Auditable(action = "CREATE", entityType = "EMPLOYEE")
    public ResponseEntity<?> create(@Valid @RequestBody EmployeeRequest request,
                                    @AuthenticationPrincipal JwtPrincipal principal) {
        if (employeeRepository.existsByTenantIdAndNameIgnoreCase(principal.tenantId(), request.name())) {
            return ResponseEntity.status(409).body(Map.of("error", "Employee with that name already exists"));
        }

        Tenant tenant = tenantRepository.getReferenceById(principal.tenantId());
        Employee employee = Employee.builder()
                .tenant(tenant)
                .name(request.name())
                .specialization(request.specialization())
                .contact(request.contact())
                .active(true)
                .build();
        return ResponseEntity.ok(employeeRepository.save(employee));
    }

    @PutMapping("/{id}")
    @Auditable(action = "UPDATE", entityType = "EMPLOYEE")
    public ResponseEntity<Employee> update(@PathVariable UUID id,
                                           @Valid @RequestBody EmployeeRequest request,
                                           @AuthenticationPrincipal JwtPrincipal principal) {
        return employeeRepository.findById(id)
                .filter(e -> e.getTenant().getId().equals(principal.tenantId()))
                .map(employee -> {
                    employee.setName(request.name());
                    employee.setSpecialization(request.specialization());
                    employee.setContact(request.contact());
                    return ResponseEntity.ok(employeeRepository.save(employee));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/activate")
    @Auditable(action = "ACTIVATE", entityType = "EMPLOYEE")
    public ResponseEntity<Employee> activate(@PathVariable UUID id,
                                             @AuthenticationPrincipal JwtPrincipal principal) {
        return employeeRepository.findById(id)
                .filter(e -> e.getTenant().getId().equals(principal.tenantId()))
                .map(employee -> {
                    employee.setActive(true);
                    return ResponseEntity.ok(employeeRepository.save(employee));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/deactivate")
    @Auditable(action = "DEACTIVATE", entityType = "EMPLOYEE")
    public ResponseEntity<Employee> deactivate(@PathVariable UUID id,
                                               @AuthenticationPrincipal JwtPrincipal principal) {
        return employeeRepository.findById(id)
                .filter(e -> e.getTenant().getId().equals(principal.tenantId()))
                .map(employee -> {
                    employee.setActive(false);
                    return ResponseEntity.ok(employeeRepository.save(employee));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record EmployeeRequest(@NotBlank String name, String specialization, String contact) {}
}
