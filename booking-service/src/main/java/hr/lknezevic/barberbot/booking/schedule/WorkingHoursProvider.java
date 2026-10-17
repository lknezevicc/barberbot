package hr.lknezevic.barberbot.booking.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WorkingHoursProvider {

    Optional<TimeRange> getWorkingHours(DayOfWeek dayOfWeek);

    default Optional<TimeRange> getWorkingHours(UUID employeeId, DayOfWeek dayOfWeek) {
        return getWorkingHours(dayOfWeek);
    }

    default boolean isWorkingDay(DayOfWeek dayOfWeek) {
        return getWorkingHours(dayOfWeek).isPresent();
    }

    default boolean isBlocked(UUID employeeId, LocalDate date) {
        return false;
    }
}
