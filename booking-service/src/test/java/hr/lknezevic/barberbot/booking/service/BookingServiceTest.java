package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.entity.Employee;
import hr.lknezevic.barberbot.booking.entity.ServiceEntity;
import hr.lknezevic.barberbot.booking.entity.Tenant;
import hr.lknezevic.barberbot.booking.repository.BookingRepository;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    @Captor
    private ArgumentCaptor<Booking> bookingCaptor;

    private static final String USER_ID = "user123";
    private static final Long CHAT_ID = 999L;
    private static final String SERVICE = "Sisanje";
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);
    private static final LocalTime TIME = LocalTime.of(14, 0);
    private static final String BARBER = "Marko";
    private static final int DURATION_MINUTES = 30;
    private static final String CUSTOMER_NAME = "Ivan Horvat";

    @Test
    void holdSlot_createsHeldBookingWithCorrectFields() {
        Tenant tenant = Tenant.builder().id(UUID.randomUUID()).name("TestSalon").build();
        Employee employee = Employee.builder().id(UUID.randomUUID()).name(BARBER).tenant(tenant).build();
        ServiceEntity serviceEntity = ServiceEntity.builder()
                .id(UUID.randomUUID()).name(SERVICE).durationMinutes(DURATION_MINUTES).tenant(tenant).build();

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        Booking result = bookingService.holdSlot(USER_ID, CHAT_ID, SERVICE, DATE, TIME, BARBER,
                DURATION_MINUTES, CUSTOMER_NAME, tenant, employee, serviceEntity);

        verify(bookingRepository).save(bookingCaptor.capture());
        Booking captured = bookingCaptor.getValue();

        assertThat(captured.getUserId()).isEqualTo(USER_ID);
        assertThat(captured.getChatId()).isEqualTo(CHAT_ID);
        assertThat(captured.getService()).isEqualTo(SERVICE);
        assertThat(captured.getDate()).isEqualTo(DATE);
        assertThat(captured.getTime()).isEqualTo(TIME);
        assertThat(captured.getBarber()).isEqualTo(BARBER);
        assertThat(captured.getStatus()).isEqualTo(BookingStatus.HELD);
        assertThat(captured.getDurationMinutes()).isEqualTo(DURATION_MINUTES);
        assertThat(captured.getHeldAt()).isNotNull();
        assertThat(captured.getCustomerName()).isEqualTo(CUSTOMER_NAME);
        assertThat(captured.getTenant()).isEqualTo(tenant);
        assertThat(captured.getEmployee()).isEqualTo(employee);
        assertThat(captured.getServiceEntity()).isEqualTo(serviceEntity);
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void confirmHold_validHeldBooking_transitionsToConfirmed() {
        UUID bookingId = UUID.randomUUID();
        Booking heldBooking = Booking.builder()
                .id(bookingId)
                .userId(USER_ID)
                .chatId(CHAT_ID)
                .service(SERVICE)
                .date(DATE)
                .time(TIME)
                .barber(BARBER)
                .status(BookingStatus.HELD)
                .heldAt(Instant.now())
                .durationMinutes(DURATION_MINUTES)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(heldBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Booking> result = bookingService.confirmHold(bookingId, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.get().getHeldAt()).isNull();
    }

    @Test
    void confirmHold_wrongUserId_returnsEmpty() {
        UUID bookingId = UUID.randomUUID();
        Booking heldBooking = Booking.builder()
                .id(bookingId)
                .userId(USER_ID)
                .chatId(CHAT_ID)
                .service(SERVICE)
                .date(DATE)
                .time(TIME)
                .barber(BARBER)
                .status(BookingStatus.HELD)
                .heldAt(Instant.now())
                .durationMinutes(DURATION_MINUTES)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(heldBooking));

        Optional<Booking> result = bookingService.confirmHold(bookingId, "wrong-user");

        assertThat(result).isEmpty();
    }

    @Test
    void confirmHold_nonHeldStatus_returnsEmpty() {
        UUID bookingId = UUID.randomUUID();
        Booking confirmedBooking = Booking.builder()
                .id(bookingId)
                .userId(USER_ID)
                .chatId(CHAT_ID)
                .service(SERVICE)
                .date(DATE)
                .time(TIME)
                .barber(BARBER)
                .status(BookingStatus.CONFIRMED)
                .durationMinutes(DURATION_MINUTES)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(confirmedBooking));

        Optional<Booking> result = bookingService.confirmHold(bookingId, USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void cancelBooking_confirmedBooking_transitionsToCancelled() {
        UUID bookingId = UUID.randomUUID();
        Booking confirmedBooking = Booking.builder()
                .id(bookingId)
                .userId(USER_ID)
                .chatId(CHAT_ID)
                .service(SERVICE)
                .date(DATE)
                .time(TIME)
                .barber(BARBER)
                .status(BookingStatus.CONFIRMED)
                .durationMinutes(DURATION_MINUTES)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Booking> result = bookingService.cancelBooking(bookingId, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelBooking_heldBooking_transitionsToCancelled() {
        UUID bookingId = UUID.randomUUID();
        Booking heldBooking = Booking.builder()
                .id(bookingId)
                .userId(USER_ID)
                .chatId(CHAT_ID)
                .service(SERVICE)
                .date(DATE)
                .time(TIME)
                .barber(BARBER)
                .status(BookingStatus.HELD)
                .heldAt(Instant.now())
                .durationMinutes(DURATION_MINUTES)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(heldBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Booking> result = bookingService.cancelBooking(bookingId, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelBooking_alreadyCancelled_returnsEmpty() {
        UUID bookingId = UUID.randomUUID();
        Booking cancelledBooking = Booking.builder()
                .id(bookingId)
                .userId(USER_ID)
                .chatId(CHAT_ID)
                .service(SERVICE)
                .date(DATE)
                .time(TIME)
                .barber(BARBER)
                .status(BookingStatus.CANCELLED)
                .durationMinutes(DURATION_MINUTES)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(cancelledBooking));

        Optional<Booking> result = bookingService.cancelBooking(bookingId, USER_ID);

        assertThat(result).isEmpty();
    }
}
