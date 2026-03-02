package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.repository.BookingRepository;
import hr.lknezevic.barberbot.booking.schedule.ServiceDuration;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotAvailabilityService {

    private static final Duration SLOT_GRANULARITY = Duration.ofMinutes(5);
    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.CONFIRMED, BookingStatus.HELD);
    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final WorkingHoursProvider workingHoursProvider;
    private final ServiceDuration serviceDuration;

    public boolean isSlotAvailable(String barber, LocalDate date, LocalTime time, Duration duration) {
        Optional<TimeRange> hours = workingHoursProvider.getWorkingHours(date.getDayOfWeek());
        if (hours.isEmpty()) return false;

        LocalTime slotEnd = time.plus(duration);
        if (!hours.get().fitsEntirely(time, slotEnd)) return false;

        return !hasOverlap(barber, date, time, slotEnd);
    }

    public List<AvailableSlot> findNextAvailableSlots(String barber, LocalDate startDate,
                                                       LocalTime fromTime, String service, int count) {
        Duration duration = serviceDuration.getDuration(service);
        List<AvailableSlot> results = new ArrayList<>();
        LocalDate currentDate = startDate;
        int daysScanned = 0;

        while (results.size() < count && daysScanned < 14) {
            Optional<TimeRange> hours = workingHoursProvider.getWorkingHours(currentDate.getDayOfWeek());
            if (hours.isPresent()) {
                LocalTime scanFrom = currentDate.equals(startDate) && fromTime != null
                        ? roundUpToGrid(fromTime)
                        : hours.get().start();

                List<AvailableSlot> slotsOnDay = findSlotsOnDay(
                        barber, currentDate, scanFrom, hours.get(), duration, count - results.size());
                results.addAll(slotsOnDay);
            }
            currentDate = currentDate.plusDays(1);
            daysScanned++;
        }

        return results;
    }

    private List<AvailableSlot> findSlotsOnDay(String barber, LocalDate date, LocalTime fromTime,
                                                TimeRange hours, Duration duration, int maxResults) {
        List<AvailableSlot> slots = new ArrayList<>();
        LocalTime cursor = fromTime.isBefore(hours.start()) ? hours.start() : fromTime;

        List<Booking> existingBookings = bookingRepository.findByBarberAndDateAndStatusIn(
                barber, date, BLOCKING_STATUSES);

        while (slots.size() < maxResults) {
            LocalTime slotEnd = cursor.plus(duration);
            if (slotEnd.isAfter(hours.end())) break;

            boolean overlaps = existingBookings.stream().anyMatch(b -> {
                LocalTime bStart = b.getTime();
                LocalTime bEnd = bStart.plusMinutes(
                        b.getDurationMinutes() > 0 ? b.getDurationMinutes() : DEFAULT_DURATION_MINUTES);
                return cursor.isBefore(bEnd) && slotEnd.isAfter(bStart);
            });

            if (!overlaps) {
                slots.add(new AvailableSlot(date, cursor));
            }

            cursor = cursor.plus(SLOT_GRANULARITY);
        }

        return slots;
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
