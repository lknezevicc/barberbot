package hr.lknezevic.barberbot.booking.schedule;

import java.time.DayOfWeek;
import java.util.Optional;

public interface WorkingHoursProvider {

    Optional<TimeRange> getWorkingHours(DayOfWeek dayOfWeek);

    default boolean isWorkingDay(DayOfWeek dayOfWeek) {
        return getWorkingHours(dayOfWeek).isPresent();
    }
}
