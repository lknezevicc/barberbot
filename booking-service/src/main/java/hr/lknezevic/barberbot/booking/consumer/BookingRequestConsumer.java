package hr.lknezevic.barberbot.booking.consumer;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.entity.Employee;
import hr.lknezevic.barberbot.booking.entity.ServiceEntity;
import hr.lknezevic.barberbot.booking.entity.Tenant;
import hr.lknezevic.barberbot.booking.publisher.BookingEventPublisher;
import hr.lknezevic.barberbot.booking.repository.EmployeeRepository;
import hr.lknezevic.barberbot.booking.repository.ServiceRepository;
import hr.lknezevic.barberbot.booking.repository.TenantRepository;
import hr.lknezevic.barberbot.booking.schedule.DatabaseServiceDuration;
import hr.lknezevic.barberbot.booking.schedule.WorkingHoursProvider;
import hr.lknezevic.barberbot.booking.schedule.TimeRange;
import hr.lknezevic.barberbot.booking.service.BookingService;
import hr.lknezevic.barberbot.booking.service.SlotAvailabilityService;
import hr.lknezevic.barberbot.booking.service.SlotAvailabilityService.AvailableSlot;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.AlternativeSlot;
import hr.lknezevic.barberbot.model.event.BookingHeldEvent;
import hr.lknezevic.barberbot.model.event.BookingRequestEvent;
import hr.lknezevic.barberbot.model.event.BookingSlotUnavailableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingRequestConsumer {

    private static final int ALTERNATIVE_SLOT_COUNT = 3;

    private final BookingService bookingService;
    private final BookingEventPublisher eventPublisher;
    private final SlotAvailabilityService slotAvailabilityService;
    private final WorkingHoursProvider workingHoursProvider;
    private final DatabaseServiceDuration serviceDuration;
    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;

    @RabbitListener(queues = RabbitQueues.BOOKING_REQUEST_QUEUE)
    public void onBookingRequest(BookingRequestEvent event) {
        log.info("Received booking request: userId={}, service={}, date={}, time={}, barber={}, tenantId={}",
                event.userId(), event.service(), event.date(), event.time(), event.barber(), event.tenantId());

        if (event.date().isBefore(LocalDate.now())) {
            log.warn("Rejected booking request for past date: {}", event.date());
            publishUnavailable(event, List.of());
            return;
        }

        UUID tenantId = parseTenantId(event.tenantId());
        Employee employee = resolveEmployee(tenantId, event.barber());
        UUID employeeId = employee != null ? employee.getId() : null;

        if (employeeId != null && workingHoursProvider.isBlocked(employeeId, event.date())) {
            log.warn("Rejected booking request: employee {} is blocked on {}", event.barber(), event.date());
            publishUnavailableWithAlternatives(event, employeeId, tenantId);
            return;
        }

        Optional<TimeRange> hours = workingHoursProvider.getWorkingHours(employeeId, event.date().getDayOfWeek());
        if (hours.isEmpty()) {
            log.warn("Rejected booking request for non-working day: {}", event.date());
            publishUnavailableWithAlternatives(event, employeeId, tenantId);
            return;
        }

        Duration duration = serviceDuration.getDuration(tenantId, event.service());
        LocalTime slotEnd = event.time().plus(duration);

        if (!hours.get().fitsEntirely(event.time(), slotEnd)) {
            log.warn("Rejected booking request: time {} doesn't fit working hours {}", event.time(), hours.get());
            publishUnavailableWithAlternatives(event, employeeId, tenantId);
            return;
        }

        boolean available = slotAvailabilityService.isSlotAvailable(
                event.barber(), event.date(), event.time(), duration, employeeId);

        if (available) {
            ServiceEntity serviceEntity = resolveService(tenantId, event.service());
            Tenant tenant = resolveTenant(tenantId);
            String resolvedBarberName = employee != null ? employee.getName() : event.barber();
            String resolvedServiceName = serviceEntity != null ? serviceEntity.getName() : event.service();

            Booking held = bookingService.holdSlot(
                    event.userId(), event.chatId(), resolvedServiceName,
                    event.date(), event.time(), resolvedBarberName,
                    serviceDuration.getDurationMinutes(tenantId, event.service()),
                    event.customerName(),
                    tenant, employee, serviceEntity);

            log.info("Slot held: bookingId={}, barber={}, date={}, time={}, customer={}",
                    held.getId(), held.getBarber(), held.getDate(), held.getTime(), held.getCustomerName());

            eventPublisher.publishHeld(new BookingHeldEvent(
                    held.getId(), held.getUserId(), held.getChatId(),
                    event.tenantId(),
                    held.getService(), held.getDate(), held.getTime(), held.getBarber()));
        } else {
            log.info("Slot unavailable: barber={}, date={}, time={}", event.barber(), event.date(), event.time());
            publishUnavailableWithAlternatives(event, employeeId, tenantId);
        }
    }

    private void publishUnavailableWithAlternatives(BookingRequestEvent event,
                                                     UUID employeeId, UUID tenantId) {
        List<AvailableSlot> alternatives = slotAvailabilityService.findNextAvailableSlots(
                event.barber(), event.date(), event.time(), event.service(),
                ALTERNATIVE_SLOT_COUNT, employeeId, tenantId);

        List<AlternativeSlot> altSlots = alternatives.stream()
                .map(s -> new AlternativeSlot(s.date(), s.time()))
                .toList();

        publishUnavailable(event, altSlots);
    }

    private void publishUnavailable(BookingRequestEvent event, List<AlternativeSlot> alternatives) {
        eventPublisher.publishSlotUnavailable(new BookingSlotUnavailableEvent(
                event.userId(), event.chatId(), event.tenantId(), event.service(),
                event.date(), event.time(), event.barber(), alternatives));
    }

    private UUID parseTenantId(String tenantIdStr) {
        if (tenantIdStr == null || tenantIdStr.isBlank()) return null;
        try {
            return UUID.fromString(tenantIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tenantId format: {}", tenantIdStr);
            return null;
        }
    }

    private Employee resolveEmployee(UUID tenantId, String barberName) {
        if (tenantId == null || barberName == null) return null;
        String trimmed = barberName.trim();

        Optional<Employee> exact = employeeRepository.findByTenantIdAndNameIgnoreCase(tenantId, trimmed);
        if (exact.isPresent()) return exact.get();

        List<Employee> partial = employeeRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, trimmed);
        if (partial.size() == 1) {
            log.info("Fuzzy-matched barber '{}' -> '{}'", trimmed, partial.getFirst().getName());
            return partial.getFirst();
        }
        if (partial.size() > 1) {
            log.warn("Ambiguous barber name '{}', matched {} employees", trimmed, partial.size());
        }
        return null;
    }

    private ServiceEntity resolveService(UUID tenantId, String serviceName) {
        if (tenantId == null || serviceName == null) return null;
        return serviceRepository.findByTenantIdAndNameIgnoreCase(tenantId, serviceName.trim())
                .orElse(null);
    }

    private Tenant resolveTenant(UUID tenantId) {
        if (tenantId == null) return null;
        return tenantRepository.findById(tenantId).orElse(null);
    }
}
