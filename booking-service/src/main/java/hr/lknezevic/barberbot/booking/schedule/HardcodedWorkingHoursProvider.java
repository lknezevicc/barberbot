package hr.lknezevic.barberbot.booking.schedule;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
public class HardcodedWorkingHoursProvider implements WorkingHoursProvider {

    private static final Map<DayOfWeek, TimeRange> SCHEDULE = new EnumMap<>(DayOfWeek.class);

    static {
        TimeRange weekday = new TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0));
        TimeRange saturday = new TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0));

        SCHEDULE.put(DayOfWeek.MONDAY, weekday);
        SCHEDULE.put(DayOfWeek.TUESDAY, weekday);
        SCHEDULE.put(DayOfWeek.WEDNESDAY, weekday);
        SCHEDULE.put(DayOfWeek.THURSDAY, weekday);
        SCHEDULE.put(DayOfWeek.FRIDAY, weekday);
        SCHEDULE.put(DayOfWeek.SATURDAY, saturday);
    }

    @Override
    public Optional<TimeRange> getWorkingHours(DayOfWeek dayOfWeek) {
        return Optional.ofNullable(SCHEDULE.get(dayOfWeek));
    }
}
