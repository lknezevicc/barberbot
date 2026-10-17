package hr.lknezevic.barberbot.booking.schedule;

import hr.lknezevic.barberbot.booking.entity.WorkingHoursEntry;
import hr.lknezevic.barberbot.booking.repository.BlockedSlotRepository;
import hr.lknezevic.barberbot.booking.repository.WorkingHoursRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DatabaseWorkingHoursProvider implements WorkingHoursProvider {

    private final WorkingHoursRepository workingHoursRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final HardcodedWorkingHoursProvider fallback;

    @Override
    public Optional<TimeRange> getWorkingHours(DayOfWeek dayOfWeek) {
        return fallback.getWorkingHours(dayOfWeek);
    }

    @Override
    public Optional<TimeRange> getWorkingHours(UUID employeeId, DayOfWeek dayOfWeek) {
        if (employeeId == null) {
            return getWorkingHours(dayOfWeek);
        }

        Optional<WorkingHoursEntry> entry = workingHoursRepository
                .findByEmployeeIdAndDayOfWeek(employeeId, dayOfWeek.getValue());

        if (entry.isPresent()) {
            return Optional.of(new TimeRange(entry.get().getStartTime(), entry.get().getEndTime()));
        }

        return getWorkingHours(dayOfWeek);
    }

    @Override
    public boolean isBlocked(UUID employeeId, LocalDate date) {
        if (employeeId == null) return false;
        return blockedSlotRepository.isFullDayBlocked(employeeId, date);
    }
}
