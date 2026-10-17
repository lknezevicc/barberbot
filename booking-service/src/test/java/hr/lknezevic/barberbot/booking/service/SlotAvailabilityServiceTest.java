package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.BlockedSlot;
import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.repository.BlockedSlotRepository;
import hr.lknezevic.barberbot.booking.repository.BookingRepository;
import hr.lknezevic.barberbot.booking.schedule.DatabaseServiceDuration;
import hr.lknezevic.barberbot.booking.schedule.TimeRange;
import hr.lknezevic.barberbot.booking.schedule.WorkingHoursProvider;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotAvailabilityServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BlockedSlotRepository blockedSlotRepository;

    @Mock
    private WorkingHoursProvider workingHoursProvider;

    @Mock
    private DatabaseServiceDuration serviceDuration;

    @InjectMocks
    private SlotAvailabilityService slotAvailabilityService;

    private static final String BARBER = "Marko";
    private static final LocalDate DATE = LocalDate.of(2026, 6, 10);
    private static final LocalTime TIME = LocalTime.of(10, 0);
    private static final Duration DURATION = Duration.ofMinutes(30);
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final TimeRange WORKING_HOURS = new TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0));
    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(BookingStatus.CONFIRMED, BookingStatus.HELD);

    @Test
    void isSlotAvailable_noConflicts_returnsTrue() {
        when(workingHoursProvider.getWorkingHours(EMPLOYEE_ID, DATE.getDayOfWeek()))
                .thenReturn(Optional.of(WORKING_HOURS));
        when(workingHoursProvider.isBlocked(EMPLOYEE_ID, DATE)).thenReturn(false);
        when(blockedSlotRepository.findByEmployeeIdAndDate(EMPLOYEE_ID, DATE)).thenReturn(List.of());
        when(bookingRepository.findByBarberAndDateAndStatusIn(BARBER, DATE, BLOCKING_STATUSES))
                .thenReturn(List.of());

        boolean result = slotAvailabilityService.isSlotAvailable(BARBER, DATE, TIME, DURATION, EMPLOYEE_ID);

        assertThat(result).isTrue();
    }

    @Test
    void isSlotAvailable_noWorkingHours_returnsFalse() {
        when(workingHoursProvider.getWorkingHours(EMPLOYEE_ID, DATE.getDayOfWeek()))
                .thenReturn(Optional.empty());

        boolean result = slotAvailabilityService.isSlotAvailable(BARBER, DATE, TIME, DURATION, EMPLOYEE_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isSlotAvailable_employeeBlockedOnDate_returnsFalse() {
        when(workingHoursProvider.getWorkingHours(EMPLOYEE_ID, DATE.getDayOfWeek()))
                .thenReturn(Optional.of(WORKING_HOURS));
        when(workingHoursProvider.isBlocked(EMPLOYEE_ID, DATE)).thenReturn(true);

        boolean result = slotAvailabilityService.isSlotAvailable(BARBER, DATE, TIME, DURATION, EMPLOYEE_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isSlotAvailable_outsideWorkingHours_returnsFalse() {
        LocalTime lateTime = LocalTime.of(16, 45);
        when(workingHoursProvider.getWorkingHours(EMPLOYEE_ID, DATE.getDayOfWeek()))
                .thenReturn(Optional.of(WORKING_HOURS));
        when(workingHoursProvider.isBlocked(EMPLOYEE_ID, DATE)).thenReturn(false);

        boolean result = slotAvailabilityService.isSlotAvailable(BARBER, DATE, lateTime, DURATION, EMPLOYEE_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isSlotAvailable_existingBookingOverlap_returnsFalse() {
        Booking existingBooking = Booking.builder()
                .id(UUID.randomUUID())
                .barber(BARBER)
                .date(DATE)
                .time(LocalTime.of(9, 45))
                .durationMinutes(30)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(workingHoursProvider.getWorkingHours(EMPLOYEE_ID, DATE.getDayOfWeek()))
                .thenReturn(Optional.of(WORKING_HOURS));
        when(workingHoursProvider.isBlocked(EMPLOYEE_ID, DATE)).thenReturn(false);
        when(blockedSlotRepository.findByEmployeeIdAndDate(EMPLOYEE_ID, DATE)).thenReturn(List.of());
        when(bookingRepository.findByBarberAndDateAndStatusIn(BARBER, DATE, BLOCKING_STATUSES))
                .thenReturn(List.of(existingBooking));

        boolean result = slotAvailabilityService.isSlotAvailable(BARBER, DATE, TIME, DURATION, EMPLOYEE_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isSlotAvailable_blockedSlotOverlap_returnsFalse() {
        BlockedSlot blockedSlot = BlockedSlot.builder()
                .id(UUID.randomUUID())
                .date(DATE)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(10, 30))
                .allDay(false)
                .build();

        when(workingHoursProvider.getWorkingHours(EMPLOYEE_ID, DATE.getDayOfWeek()))
                .thenReturn(Optional.of(WORKING_HOURS));
        when(workingHoursProvider.isBlocked(EMPLOYEE_ID, DATE)).thenReturn(false);
        when(blockedSlotRepository.findByEmployeeIdAndDate(EMPLOYEE_ID, DATE))
                .thenReturn(List.of(blockedSlot));

        boolean result = slotAvailabilityService.isSlotAvailable(BARBER, DATE, TIME, DURATION, EMPLOYEE_ID);

        assertThat(result).isFalse();
    }

    @Test
    void findNextAvailableSlots_thirtyMinService_usesThirtyMinStep() {
        UUID tenantId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 6, 10);
        Duration thirtyMin = Duration.ofMinutes(30);

        when(serviceDuration.getDuration(tenantId, "Sisanje")).thenReturn(thirtyMin);
        when(workingHoursProvider.getWorkingHours(EMPLOYEE_ID, startDate.getDayOfWeek()))
                .thenReturn(Optional.of(WORKING_HOURS));
        when(workingHoursProvider.isBlocked(EMPLOYEE_ID, startDate)).thenReturn(false);
        when(bookingRepository.findByBarberAndDateAndStatusIn(BARBER, startDate, BLOCKING_STATUSES))
                .thenReturn(List.of());
        when(blockedSlotRepository.findByEmployeeIdAndDate(EMPLOYEE_ID, startDate))
                .thenReturn(List.of());

        List<SlotAvailabilityService.AvailableSlot> slots = slotAvailabilityService.findNextAvailableSlots(
                BARBER, startDate, LocalTime.of(9, 0), "Sisanje", 3, EMPLOYEE_ID, tenantId);

        assertThat(slots).hasSize(3);
        assertThat(slots.get(0).time()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(1).time()).isEqualTo(LocalTime.of(9, 30));
        assertThat(slots.get(2).time()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void findNextAvailableSlots_skipsBlockedDays() {
        UUID tenantId = UUID.randomUUID();
        LocalDate blockedDate = LocalDate.of(2026, 6, 10);
        LocalDate nextDate = blockedDate.plusDays(1);

        when(serviceDuration.getDuration(tenantId, "Sisanje")).thenReturn(DURATION);
        when(workingHoursProvider.getWorkingHours(EMPLOYEE_ID, blockedDate.getDayOfWeek()))
                .thenReturn(Optional.of(WORKING_HOURS));
        when(workingHoursProvider.isBlocked(EMPLOYEE_ID, blockedDate)).thenReturn(true);
        when(workingHoursProvider.getWorkingHours(EMPLOYEE_ID, nextDate.getDayOfWeek()))
                .thenReturn(Optional.of(WORKING_HOURS));
        when(workingHoursProvider.isBlocked(EMPLOYEE_ID, nextDate)).thenReturn(false);
        when(bookingRepository.findByBarberAndDateAndStatusIn(BARBER, nextDate, BLOCKING_STATUSES))
                .thenReturn(List.of());
        when(blockedSlotRepository.findByEmployeeIdAndDate(EMPLOYEE_ID, nextDate))
                .thenReturn(List.of());

        List<SlotAvailabilityService.AvailableSlot> slots = slotAvailabilityService.findNextAvailableSlots(
                BARBER, blockedDate, LocalTime.of(9, 0), "Sisanje", 1, EMPLOYEE_ID, tenantId);

        assertThat(slots).hasSize(1);
        assertThat(slots.getFirst().date()).isEqualTo(nextDate);
    }
}
