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
import hr.lknezevic.barberbot.booking.schedule.TimeRange;
import hr.lknezevic.barberbot.booking.schedule.WorkingHoursProvider;
import hr.lknezevic.barberbot.booking.service.BookingService;
import hr.lknezevic.barberbot.booking.service.SlotAvailabilityService;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import hr.lknezevic.barberbot.model.event.BookingHeldEvent;
import hr.lknezevic.barberbot.model.event.BookingRequestEvent;
import hr.lknezevic.barberbot.model.event.BookingSlotUnavailableEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingRequestConsumerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingEventPublisher eventPublisher;

    @Mock
    private SlotAvailabilityService slotAvailabilityService;

    @Mock
    private WorkingHoursProvider workingHoursProvider;

    @Mock
    private DatabaseServiceDuration serviceDuration;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private BookingRequestConsumer bookingRequestConsumer;

    @Captor
    private ArgumentCaptor<BookingHeldEvent> heldEventCaptor;

    @Captor
    private ArgumentCaptor<BookingSlotUnavailableEvent> unavailableEventCaptor;

    private static final String USER_ID = "user123";
    private static final Long CHAT_ID = 999L;
    private static final String SERVICE_NAME = "Sisanje";
    private static final LocalDate DATE = LocalDate.now().plusDays(2);
    private static final LocalTime TIME = LocalTime.of(10, 0);
    private static final String BARBER = "Marko";
    private static final String CUSTOMER_NAME = "Ivan Horvat";
    private static final Duration DURATION = Duration.ofMinutes(30);

    @Test
    void onBookingRequest_slotAvailable_holdsSlotAndPublishesHeldEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        Tenant tenant = Tenant.builder().id(tenantId).name("TestSalon").build();
        Employee employee = Employee.builder().id(employeeId).name(BARBER).tenant(tenant).active(true).build();
        ServiceEntity serviceEntity = ServiceEntity.builder()
                .id(UUID.randomUUID()).name(SERVICE_NAME).durationMinutes(30).tenant(tenant).build();
        TimeRange workingHours = new TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0));

        BookingRequestEvent event = new BookingRequestEvent(
                USER_ID, CHAT_ID, tenantId.toString(), SERVICE_NAME, DATE, TIME, BARBER, CUSTOMER_NAME);

        when(employeeRepository.findByTenantIdAndNameIgnoreCase(tenantId, BARBER))
                .thenReturn(Optional.of(employee));
        when(workingHoursProvider.isBlocked(employeeId, DATE)).thenReturn(false);
        when(workingHoursProvider.getWorkingHours(employeeId, DATE.getDayOfWeek()))
                .thenReturn(Optional.of(workingHours));
        when(serviceDuration.getDuration(tenantId, SERVICE_NAME)).thenReturn(DURATION);
        when(slotAvailabilityService.isSlotAvailable(BARBER, DATE, TIME, DURATION, employeeId))
                .thenReturn(true);
        when(serviceRepository.findByTenantIdAndNameIgnoreCase(tenantId, SERVICE_NAME))
                .thenReturn(Optional.of(serviceEntity));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(serviceDuration.getDurationMinutes(tenantId, SERVICE_NAME)).thenReturn(30);

        Booking heldBooking = Booking.builder()
                .id(bookingId)
                .userId(USER_ID)
                .chatId(CHAT_ID)
                .service(SERVICE_NAME)
                .date(DATE)
                .time(TIME)
                .barber(BARBER)
                .status(BookingStatus.HELD)
                .durationMinutes(30)
                .customerName(CUSTOMER_NAME)
                .tenant(tenant)
                .employee(employee)
                .serviceEntity(serviceEntity)
                .build();

        when(bookingService.holdSlot(
                eq(USER_ID), eq(CHAT_ID), eq(SERVICE_NAME), eq(DATE), eq(TIME), eq(BARBER),
                eq(30), eq(CUSTOMER_NAME), eq(tenant), eq(employee), eq(serviceEntity)))
                .thenReturn(heldBooking);

        bookingRequestConsumer.onBookingRequest(event);

        verify(eventPublisher).publishHeld(heldEventCaptor.capture());
        BookingHeldEvent heldEvent = heldEventCaptor.getValue();
        assertThat(heldEvent.bookingId()).isEqualTo(bookingId);
        assertThat(heldEvent.userId()).isEqualTo(USER_ID);
        assertThat(heldEvent.chatId()).isEqualTo(CHAT_ID);
        assertThat(heldEvent.service()).isEqualTo(SERVICE_NAME);
        assertThat(heldEvent.date()).isEqualTo(DATE);
        assertThat(heldEvent.time()).isEqualTo(TIME);
        assertThat(heldEvent.barber()).isEqualTo(BARBER);
    }

    @Test
    void onBookingRequest_slotUnavailable_publishesUnavailableEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        Tenant tenant = Tenant.builder().id(tenantId).name("TestSalon").build();
        Employee employee = Employee.builder().id(employeeId).name(BARBER).tenant(tenant).active(true).build();
        TimeRange workingHours = new TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0));

        BookingRequestEvent event = new BookingRequestEvent(
                USER_ID, CHAT_ID, tenantId.toString(), SERVICE_NAME, DATE, TIME, BARBER, CUSTOMER_NAME);

        when(employeeRepository.findByTenantIdAndNameIgnoreCase(tenantId, BARBER))
                .thenReturn(Optional.of(employee));
        when(workingHoursProvider.isBlocked(employeeId, DATE)).thenReturn(false);
        when(workingHoursProvider.getWorkingHours(employeeId, DATE.getDayOfWeek()))
                .thenReturn(Optional.of(workingHours));
        when(serviceDuration.getDuration(tenantId, SERVICE_NAME)).thenReturn(DURATION);
        when(slotAvailabilityService.isSlotAvailable(BARBER, DATE, TIME, DURATION, employeeId))
                .thenReturn(false);
        when(slotAvailabilityService.findNextAvailableSlots(BARBER, DATE, TIME, SERVICE_NAME, 3, employeeId, tenantId))
                .thenReturn(List.of());

        bookingRequestConsumer.onBookingRequest(event);

        verify(eventPublisher).publishSlotUnavailable(unavailableEventCaptor.capture());
        BookingSlotUnavailableEvent unavailableEvent = unavailableEventCaptor.getValue();
        assertThat(unavailableEvent.userId()).isEqualTo(USER_ID);
        assertThat(unavailableEvent.chatId()).isEqualTo(CHAT_ID);
        assertThat(unavailableEvent.originalDate()).isEqualTo(DATE);
        assertThat(unavailableEvent.originalTime()).isEqualTo(TIME);
    }

    @Test
    void onBookingRequest_pastDate_publishesUnavailableImmediately() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        UUID tenantId = UUID.randomUUID();

        BookingRequestEvent event = new BookingRequestEvent(
                USER_ID, CHAT_ID, tenantId.toString(), SERVICE_NAME, pastDate, TIME, BARBER, CUSTOMER_NAME);

        bookingRequestConsumer.onBookingRequest(event);

        verify(eventPublisher).publishSlotUnavailable(unavailableEventCaptor.capture());
        BookingSlotUnavailableEvent unavailableEvent = unavailableEventCaptor.getValue();
        assertThat(unavailableEvent.userId()).isEqualTo(USER_ID);
        assertThat(unavailableEvent.originalDate()).isEqualTo(pastDate);
        assertThat(unavailableEvent.alternatives()).isEmpty();
    }

    @Test
    void onBookingRequest_nonWorkingDay_publishesUnavailableWithAlternatives() {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        Tenant tenant = Tenant.builder().id(tenantId).name("TestSalon").build();
        Employee employee = Employee.builder().id(employeeId).name(BARBER).tenant(tenant).active(true).build();

        BookingRequestEvent event = new BookingRequestEvent(
                USER_ID, CHAT_ID, tenantId.toString(), SERVICE_NAME, DATE, TIME, BARBER, CUSTOMER_NAME);

        when(employeeRepository.findByTenantIdAndNameIgnoreCase(tenantId, BARBER))
                .thenReturn(Optional.of(employee));
        when(workingHoursProvider.isBlocked(employeeId, DATE)).thenReturn(false);
        when(workingHoursProvider.getWorkingHours(employeeId, DATE.getDayOfWeek()))
                .thenReturn(Optional.empty());
        when(slotAvailabilityService.findNextAvailableSlots(BARBER, DATE, TIME, SERVICE_NAME, 3, employeeId, tenantId))
                .thenReturn(List.of());

        bookingRequestConsumer.onBookingRequest(event);

        verify(eventPublisher).publishSlotUnavailable(unavailableEventCaptor.capture());
        BookingSlotUnavailableEvent unavailableEvent = unavailableEventCaptor.getValue();
        assertThat(unavailableEvent.userId()).isEqualTo(USER_ID);
        assertThat(unavailableEvent.barber()).isEqualTo(BARBER);
    }
}
