package hr.lknezevic.barberbot.admin.controller;

import hr.lknezevic.barberbot.admin.audit.Auditable;
import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.entity.BlockedSlot;
import hr.lknezevic.barberbot.admin.entity.Employee;
import hr.lknezevic.barberbot.admin.entity.Tenant;
import hr.lknezevic.barberbot.admin.entity.WorkingHoursEntry;
import hr.lknezevic.barberbot.admin.repository.BlockedSlotRepository;
import hr.lknezevic.barberbot.admin.repository.EmployeeRepository;
import hr.lknezevic.barberbot.admin.repository.TenantRepository;
import hr.lknezevic.barberbot.admin.repository.WorkingHoursRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final WorkingHoursRepository workingHoursRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantRepository tenantRepository;

    @GetMapping("/working-hours")
    public List<WorkingHoursEntry> listWorkingHours(@AuthenticationPrincipal JwtPrincipal principal) {
        return workingHoursRepository.findByTenantId(principal.tenantId());
    }

    @GetMapping("/working-hours/{employeeId}")
    public List<WorkingHoursEntry> getEmployeeHours(@PathVariable UUID employeeId,
                                                    @AuthenticationPrincipal JwtPrincipal principal) {
        Employee emp = employeeRepository.findById(employeeId)
                .filter(e -> e.getTenant().getId().equals(principal.tenantId()))
                .orElse(null);
        if (emp == null) return List.of();
        return workingHoursRepository.findByEmployeeId(employeeId);
    }

    @PutMapping("/working-hours")
    @Auditable(action = "SET", entityType = "WORKING_HOURS")
    public ResponseEntity<?> setWorkingHours(
            @Valid @RequestBody WorkingHoursRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        if (!request.startTime().isBefore(request.endTime())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Start time must be before end time"));
        }

        Employee employee = employeeRepository.findById(request.employeeId())
                .filter(e -> e.getTenant().getId().equals(principal.tenantId()))
                .orElse(null);
        if (employee == null) return ResponseEntity.notFound().build();

        Tenant tenant = tenantRepository.getReferenceById(principal.tenantId());

        WorkingHoursEntry entry = workingHoursRepository
                .findByEmployeeIdAndDayOfWeek(request.employeeId(), request.dayOfWeek())
                .orElse(WorkingHoursEntry.builder()
                        .tenant(tenant)
                        .employee(employee)
                        .dayOfWeek(request.dayOfWeek())
                        .build());

        entry.setStartTime(request.startTime());
        entry.setEndTime(request.endTime());
        return ResponseEntity.ok(workingHoursRepository.save(entry));
    }

    @DeleteMapping("/working-hours/{employeeId}/{dayOfWeek}")
    @Auditable(action = "DELETE", entityType = "WORKING_HOURS")
    public ResponseEntity<Void> deleteWorkingHours(@PathVariable UUID employeeId,
                                                   @PathVariable int dayOfWeek,
                                                   @AuthenticationPrincipal JwtPrincipal principal) {
        Employee emp = employeeRepository.findById(employeeId)
                .filter(e -> e.getTenant().getId().equals(principal.tenantId()))
                .orElse(null);
        if (emp == null) return ResponseEntity.notFound().build();
        workingHoursRepository.findByEmployeeIdAndDayOfWeek(employeeId, dayOfWeek)
                .ifPresent(workingHoursRepository::delete);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocked-slots")
    public List<BlockedSlot> listBlockedSlots(@AuthenticationPrincipal JwtPrincipal principal) {
        return blockedSlotRepository.findByTenantId(principal.tenantId());
    }

    @PostMapping("/blocked-slots")
    @Auditable(action = "CREATE", entityType = "BLOCKED_SLOT")
    public ResponseEntity<BlockedSlot> createBlockedSlot(
            @Valid @RequestBody BlockedSlotRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .filter(e -> e.getTenant().getId().equals(principal.tenantId()))
                .orElse(null);
        if (employee == null) return ResponseEntity.notFound().build();

        Tenant tenant = tenantRepository.getReferenceById(principal.tenantId());
        BlockedSlot slot = BlockedSlot.builder()
                .tenant(tenant)
                .employee(employee)
                .date(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .reason(request.reason())
                .allDay(request.allDay() != null && request.allDay())
                .build();
        return ResponseEntity.ok(blockedSlotRepository.save(slot));
    }

    @DeleteMapping("/blocked-slots/{id}")
    @Auditable(action = "DELETE", entityType = "BLOCKED_SLOT")
    public ResponseEntity<Void> deleteBlockedSlot(@PathVariable UUID id,
                                                  @AuthenticationPrincipal JwtPrincipal principal) {
        blockedSlotRepository.findById(id)
                .filter(s -> s.getTenant().getId().equals(principal.tenantId()))
                .ifPresent(blockedSlotRepository::delete);
        return ResponseEntity.noContent().build();
    }

    public record WorkingHoursRequest(
            @NotNull UUID employeeId,
            @Min(1) @Max(7) int dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime
    ) {}

    public record BlockedSlotRequest(
            @NotNull UUID employeeId,
            @NotNull LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String reason,
            Boolean allDay
    ) {}
}
