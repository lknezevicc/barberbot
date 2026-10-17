package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.AppUser;
import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.entity.Tenant;
import hr.lknezevic.barberbot.booking.repository.AppUserRepository;
import hr.lknezevic.barberbot.booking.repository.BookingRepository;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReminderServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BookingReminderService bookingReminderService;

    @Test
    void notifyAdminOnNewBooking_sendsNotificationToAdminsWithChatId() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder().id(tenantId).name("TestSalon").build();

        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .service("Sisanje")
                .barber("Marko")
                .date(LocalDate.of(2026, 6, 15))
                .time(LocalTime.of(10, 0))
                .tenant(tenant)
                .chatId(100L)
                .userId("user1")
                .build();

        AppUser admin = AppUser.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .chatId(555L)
                .telegramId("admin1")
                .role("ADMIN")
                .build();

        when(appUserRepository.findByTenantIdAndRole(tenantId, "ADMIN")).thenReturn(List.of(admin));

        bookingReminderService.notifyAdminOnNewBooking(booking);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitQueues.EXCHANGE),
                eq(RabbitQueues.REPLY_OUTBOUND_ROUTING_KEY),
                any(TelegramReplyEvent.class));
    }

    @Test
    void notifyAdminOnNewBooking_skipsAdminsWithoutChatId() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder().id(tenantId).name("TestSalon").build();

        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .service("Sisanje")
                .barber("Marko")
                .date(LocalDate.of(2026, 6, 15))
                .time(LocalTime.of(10, 0))
                .tenant(tenant)
                .chatId(100L)
                .userId("user1")
                .build();

        AppUser adminNullChat = AppUser.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .chatId(null)
                .telegramId("admin1")
                .role("ADMIN")
                .build();

        AppUser adminZeroChat = AppUser.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .chatId(0L)
                .telegramId("admin2")
                .role("ADMIN")
                .build();

        when(appUserRepository.findByTenantIdAndRole(tenantId, "ADMIN"))
                .thenReturn(List.of(adminNullChat, adminZeroChat));

        bookingReminderService.notifyAdminOnNewBooking(booking);

        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class), any(String.class), any(TelegramReplyEvent.class));
    }

    @Test
    void notifyAdminOnNewBooking_nullTenant_doesNothing() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .service("Sisanje")
                .barber("Marko")
                .date(LocalDate.of(2026, 6, 15))
                .time(LocalTime.of(10, 0))
                .tenant(null)
                .chatId(100L)
                .userId("user1")
                .build();

        bookingReminderService.notifyAdminOnNewBooking(booking);

        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class), any(String.class), any(TelegramReplyEvent.class));
    }

    @Test
    void sendReminders_calledTwice_deduplicatesMessages() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder().id(tenantId).name("TestSalon").build();
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        Booking tomorrowBooking = Booking.builder()
                .id(UUID.randomUUID())
                .service("Sisanje")
                .barber("Marko")
                .date(tomorrow)
                .time(LocalTime.of(10, 0))
                .chatId(100L)
                .userId("user1")
                .status(BookingStatus.CONFIRMED)
                .tenant(tenant)
                .build();

        when(bookingRepository.findByDateAndStatus(tomorrow, BookingStatus.CONFIRMED))
                .thenReturn(List.of(tomorrowBooking));
        when(bookingRepository.findByDateAndStatus(LocalDate.now(), BookingStatus.CONFIRMED))
                .thenReturn(List.of());

        bookingReminderService.sendReminders();
        bookingReminderService.sendReminders();

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitQueues.EXCHANGE),
                eq(RabbitQueues.REPLY_OUTBOUND_ROUTING_KEY),
                any(TelegramReplyEvent.class));
    }
}
