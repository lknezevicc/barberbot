package hr.lknezevic.barberbot.admin.controller;

import hr.lknezevic.barberbot.admin.audit.Auditable;
import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.entity.BlockedSlot;
import hr.lknezevic.barberbot.admin.entity.Booking;
import hr.lknezevic.barberbot.admin.entity.Employee;
import hr.lknezevic.barberbot.admin.entity.ServiceEntity;
import hr.lknezevic.barberbot.admin.entity.WorkingHoursEntry;
import hr.lknezevic.barberbot.admin.repository.BlockedSlotRepository;
import hr.lknezevic.barberbot.admin.repository.BookingRepository;
import hr.lknezevic.barberbot.admin.repository.EmployeeRepository;
import hr.lknezevic.barberbot.admin.repository.ServiceRepository;
import hr.lknezevic.barberbot.admin.repository.TenantRepository;
import hr.lknezevic.barberbot.admin.repository.WorkingHoursRepository;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) LocalDate date,
                                  @RequestParam(required = false) String status,
                                  @AuthenticationPrincipal JwtPrincipal principal) {
        UUID tenantId = principal.tenantId();
        try {
            if (date != null && status != null) {
                return ResponseEntity.ok(bookingRepository.findByTenantIdAndDateAndStatus(tenantId, date, BookingStatus.valueOf(status)));
            } else if (date != null) {
                return ResponseEntity.ok(bookingRepository.findByTenantIdAndDate(tenantId, date));
            } else if (status != null) {
                return ResponseEntity.ok(bookingRepository.findByTenantIdAndStatus(tenantId, BookingStatus.valueOf(status)));
            }
            return ResponseEntity.ok(bookingRepository.findByTenantIdOrderByDateDescTimeDesc(tenantId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid booking status: " + status));
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID tid = principal.tenantId();
        LocalDate today = LocalDate.now();
        long todayCount = bookingRepository.countByTenantIdAndDateAndStatus(tid, today, BookingStatus.CONFIRMED);
        long totalConfirmed = bookingRepository.countByTenantIdAndStatus(tid, BookingStatus.CONFIRMED);
        long totalCancelled = bookingRepository.countByTenantIdAndStatus(tid, BookingStatus.CANCELLED);
        long total = totalConfirmed + totalCancelled;
        double rate = total > 0 ? (double) totalCancelled / total * 100 : 0;

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        long thisWeekBookings = bookingRepository.countByTenantIdAndStatusAndDateBetween(
                tid, BookingStatus.CONFIRMED, weekStart, weekEnd);

        List<Booking> weekBookings = bookingRepository.findByTenantIdAndDateBetween(tid, weekStart, weekEnd);
        double weeklyRevenue = weekBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> b.getServiceEntity() != null)
                .mapToDouble(b -> b.getServiceEntity().getPrice().doubleValue())
                .sum();

        String mostPopularBarber = bookingRepository.findMostPopularBarber(tid).orElse(null);
        String mostPopularService = bookingRepository.findMostPopularService(tid).orElse(null);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayBookings", todayCount);
        stats.put("totalConfirmed", totalConfirmed);
        stats.put("totalCancelled", totalCancelled);
        stats.put("cancellationRate", Math.round(rate * 10.0) / 10.0);
        stats.put("thisWeekBookings", thisWeekBookings);
        stats.put("weeklyRevenue", Math.round(weeklyRevenue * 100.0) / 100.0);
        stats.put("mostPopularBarber", mostPopularBarber);
        stats.put("mostPopularService", mostPopularService);
        return stats;
    }

    @GetMapping("/recent")
    public List<Booking> recent(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID tid = principal.tenantId();
        List<Booking> upcoming = bookingRepository.findByTenantIdAndStatusAndDateGreaterThanEqual(
                tid, BookingStatus.CONFIRMED, LocalDate.now());
        return upcoming.stream()
                .sorted((a, b) -> {
                    int cmp = a.getDate().compareTo(b.getDate());
                    return cmp != 0 ? cmp : a.getTime().compareTo(b.getTime());
                })
                .limit(10)
                .toList();
    }

    @GetMapping("/calendar")
    public ResponseEntity<CalendarResponse> calendar(
            @RequestParam(required = false) LocalDate weekStart,
            @AuthenticationPrincipal JwtPrincipal principal) {
        UUID tenantId = principal.tenantId();

        if (weekStart == null) {
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Employee> employees = employeeRepository.findByTenantIdAndActiveTrue(tenantId);
        List<WorkingHoursEntry> workingHours = workingHoursRepository.findByTenantId(tenantId);
        List<BlockedSlot> blockedSlots = blockedSlotRepository.findByTenantIdAndDateBetween(tenantId, weekStart, weekEnd);
        List<Booking> bookings = bookingRepository.findByTenantIdAndDateBetween(tenantId, weekStart, weekEnd);

        return ResponseEntity.ok(new CalendarResponse(employees, workingHours, blockedSlots, bookings));
    }

    @PostMapping
    @Auditable(action = "CREATE", entityType = "BOOKING")
    public ResponseEntity<?> create(@RequestBody CreateBookingRequest request,
                                    @AuthenticationPrincipal JwtPrincipal principal) {
        UUID tenantId = principal.tenantId();

        Employee employee = employeeRepository.findById(request.employeeId())
                .filter(e -> e.getTenant().getId().equals(tenantId))
                .orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Employee not found or not owned by tenant"));
        }

        ServiceEntity service = serviceRepository.findById(request.serviceId())
                .filter(s -> s.getTenant().getId().equals(tenantId))
                .orElse(null);
        if (service == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Service not found or not owned by tenant"));
        }

        Booking booking = Booking.builder()
                .tenant(tenantRepository.getReferenceById(tenantId))
                .employee(employee)
                .serviceEntity(service)
                .userId(request.customerName() != null ? request.customerName() : "admin-created")
                .customerName(request.customerName())
                .chatId(0L)
                .service(service.getName())
                .barber(employee.getName())
                .date(request.date())
                .time(request.time())
                .durationMinutes(service.getDurationMinutes())
                .status(BookingStatus.CONFIRMED)
                .build();

        return ResponseEntity.ok(bookingRepository.save(booking));
    }

    @PutMapping("/{id}/cancel")
    @Auditable(action = "CANCEL", entityType = "BOOKING")
    public ResponseEntity<Booking> cancel(@PathVariable UUID id,
                                          @AuthenticationPrincipal JwtPrincipal principal) {
        return bookingRepository.findById(id)
                .filter(b -> b.getTenant() != null && b.getTenant().getId().equals(principal.tenantId()))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.HELD)
                .map(booking -> {
                    booking.setStatus(BookingStatus.CANCELLED);
                    return ResponseEntity.ok(bookingRepository.save(booking));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/reschedule")
    @Auditable(action = "RESCHEDULE", entityType = "BOOKING")
    public ResponseEntity<?> reschedule(@PathVariable UUID id,
                                        @RequestBody RescheduleRequest request,
                                        @AuthenticationPrincipal JwtPrincipal principal) {
        UUID tenantId = principal.tenantId();

        return bookingRepository.findById(id)
                .filter(b -> b.getTenant() != null && b.getTenant().getId().equals(tenantId))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .<ResponseEntity<?>>map(booking -> {
                    List<Booking> existingOnDate = bookingRepository.findByTenantIdAndDateAndStatus(
                            tenantId, request.date(), BookingStatus.CONFIRMED);

                    LocalTime newStart = request.time();
                    LocalTime newEnd = newStart.plusMinutes(booking.getDurationMinutes());

                    boolean conflict = existingOnDate.stream()
                            .filter(b -> !b.getId().equals(id))
                            .filter(b -> b.getBarber().equals(booking.getBarber()))
                            .anyMatch(b -> {
                                LocalTime bStart = b.getTime();
                                LocalTime bEnd = bStart.plusMinutes(b.getDurationMinutes());
                                return newStart.isBefore(bEnd) && newEnd.isAfter(bStart);
                            });

                    if (conflict) {
                        return ResponseEntity.status(409).body(Map.of("error", "Time slot conflicts with existing booking"));
                    }

                    booking.setDate(request.date());
                    booking.setTime(request.time());
                    return ResponseEntity.ok(bookingRepository.save(booking));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record RescheduleRequest(LocalDate date, LocalTime time) {}

    public record CreateBookingRequest(UUID employeeId, UUID serviceId, LocalDate date, LocalTime time, String customerName) {}

    public record CalendarResponse(
            List<Employee> employees,
            List<WorkingHoursEntry> workingHours,
            List<BlockedSlot> blockedSlots,
            List<Booking> bookings
    ) {}
}
