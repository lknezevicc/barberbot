package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.BlockedSlot;
import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.repository.BlockedSlotRepository;
import hr.lknezevic.barberbot.booking.repository.BookingRepository;
import hr.lknezevic.barberbot.booking.schedule.DatabaseServiceDuration;
import hr.lknezevic.barberbot.booking.schedule.TimeRange;
import hr.lknezevic.barberbot.booking.schedule.WorkingHoursProvider;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotAvailabilityService {

    private static final Duration SLOT_GRANULARITY = Duration.ofMinutes(5);
    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.CONFIRMED, BookingStatus.HELD);
    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final WorkingHoursProvider workingHoursProvider;
    private final DatabaseServiceDuration serviceDuration;

    public boolean isSlotAvailable(String barber, LocalDate date, LocalTime time,
                                   Duration duration, UUID employeeId) {
        Optional<TimeRange> hours = workingHoursProvider.getWorkingHours(employeeId, date.getDayOfWeek());
        if (hours.isEmpty()) return false;

        if (employeeId != null && workingHoursProvider.isBlocked(employeeId, date)) return false;

        LocalTime slotEnd = time.plus(duration);
        if (!hours.get().fitsEntirely(time, slotEnd)) return false;

        if (employeeId != null && isTimeBlockedBySlot(employeeId, date, time, slotEnd)) return false;

        return !hasOverlap(barber, date, time, slotEnd);
    }

    @Deprecated
    public boolean isSlotAvailable(String barber, LocalDate date, LocalTime time, Duration duration) {
        return isSlotAvailable(barber, date, time, duration, null);
    }

    public List<AvailableSlot> findNextAvailableSlots(String barber, LocalDate startDate,
                                                       LocalTime fromTime, String service,
                                                       int count, UUID employeeId, UUID tenantId) {
        Duration duration = serviceDuration.getDuration(tenantId, service);
        List<AvailableSlot> results = new ArrayList<>();
        LocalDate currentDate = startDate;
        int daysScanned = 0;

        while (results.size() < count && daysScanned < 14) {
            Optional<TimeRange> hours = workingHoursProvider.getWorkingHours(employeeId, currentDate.getDayOfWeek());

            boolean dayBlocked = employeeId != null && workingHoursProvider.isBlocked(employeeId, currentDate);

            if (hours.isPresent() && !dayBlocked) {
                LocalTime scanFrom = currentDate.equals(startDate) && fromTime != null
                        ? roundUpToGrid(fromTime)
                        : hours.get().start();

                List<AvailableSlot> slotsOnDay = findSlotsOnDay(
                        barber, currentDate, scanFrom, hours.get(), duration,
                        count - results.size(), employeeId);
                results.addAll(slotsOnDay);
            }
            currentDate = currentDate.plusDays(1);
            daysScanned++;
        }

        return results;
    }

    @Deprecated
    public List<AvailableSlot> findNextAvailableSlots(String barber, LocalDate startDate,
                                                       LocalTime fromTime, String service, int count) {
        return findNextAvailableSlots(barber, startDate, fromTime, service, count, null, null);
    }

    private List<AvailableSlot> findSlotsOnDay(String barber, LocalDate date, LocalTime fromTime,
                                                TimeRange hours, Duration duration, int maxResults,
                                                UUID employeeId) {
        List<AvailableSlot> slots = new ArrayList<>();
        LocalTime cursor = fromTime.isBefore(hours.start()) ? hours.start() : fromTime;

        Duration step = duration.toMinutes() >= 30 ? Duration.ofMinutes(30) : Duration.ofMinutes(15);

        List<Booking> existingBookings = bookingRepository.findByBarberAndDateAndStatusIn(
                barber, date, BLOCKING_STATUSES);

        List<BlockedSlot> blockedSlots = employeeId != null
                ? blockedSlotRepository.findByEmployeeIdAndDate(employeeId, date)
                : List.of();

        while (slots.size() < maxResults) {
            LocalTime slotEnd = cursor.plus(duration);
            if (slotEnd.isAfter(hours.end())) break;

            final LocalTime c = cursor;
            boolean overlaps = existingBookings.stream().anyMatch(b -> {
                LocalTime bStart = b.getTime();
                LocalTime bEnd = bStart.plusMinutes(
                        b.getDurationMinutes() > 0 ? b.getDurationMinutes() : DEFAULT_DURATION_MINUTES);
                return c.isBefore(bEnd) && slotEnd.isAfter(bStart);
            });

            if (!overlaps) {
                boolean blocked = blockedSlots.stream().anyMatch(bs -> {
                    if (bs.isAllDay()) return true;
                    if (bs.getStartTime() == null || bs.getEndTime() == null) return false;
                    return c.isBefore(bs.getEndTime()) && slotEnd.isAfter(bs.getStartTime());
                });
                if (!blocked) {
                    slots.add(new AvailableSlot(date, c));
                }
            }

            cursor = cursor.plus(step);
        }

        return slots;
    }

    private boolean isTimeBlockedBySlot(UUID employeeId, LocalDate date, LocalTime start, LocalTime end) {
        List<BlockedSlot> slots = blockedSlotRepository.findByEmployeeIdAndDate(employeeId, date);
        return slots.stream().anyMatch(bs -> {
            if (bs.isAllDay()) return true;
            if (bs.getStartTime() == null || bs.getEndTime() == null) return false;
            return start.isBefore(bs.getEndTime()) && end.isAfter(bs.getStartTime());
        });
    }

    private boolean hasOverlap(String barber, LocalDate date, LocalTime newStart, LocalTime newEnd) {
        List<Booking> existing = bookingRepository.findByBarberAndDateAndStatusIn(
                barber, date, BLOCKING_STATUSES);

        return existing.stream().anyMatch(b -> {
            LocalTime bStart = b.getTime();
            LocalTime bEnd = bStart.plusMinutes(
                    b.getDurationMinutes() > 0 ? b.getDurationMinutes() : DEFAULT_DURATION_MINUTES);
            return newStart.isBefore(bEnd) && newEnd.isAfter(bStart);
        });
    }

    private LocalTime roundUpToGrid(LocalTime time) {
        int minutes = time.getMinute();
        int remainder = minutes % 5;
        if (remainder == 0) return time;
        return time.plusMinutes(5 - remainder).withSecond(0).withNano(0);
    }

    public record AvailableSlot(LocalDate date, LocalTime time) {}
}
