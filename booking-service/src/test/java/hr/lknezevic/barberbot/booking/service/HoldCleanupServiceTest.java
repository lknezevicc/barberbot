package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.repository.BookingRepository;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class HoldCleanupServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private HoldCleanupService holdCleanupService;

    @Test
    void cleanupExpiredHolds_withExpiredBookings_deletesAll() {
        Booking expired1 = Booking.builder()
                .id(UUID.randomUUID())
                .userId("user1")
                .chatId(100L)
                .service("Sisanje")
                .barber("Marko")
                .status(BookingStatus.HELD)
                .heldAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        Booking expired2 = Booking.builder()
                .id(UUID.randomUUID())
                .userId("user2")
                .chatId(200L)
                .service("Brijanje")
                .barber("Ivan")
                .status(BookingStatus.HELD)
                .heldAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();

        List<Booking> expiredList = List.of(expired1, expired2);

        when(bookingRepository.findByStatusAndHeldAtBefore(eq(BookingStatus.HELD), any(Instant.class)))
                .thenReturn(expiredList);

        holdCleanupService.cleanupExpiredHolds();

        verify(bookingRepository).deleteAll(expiredList);
    }

    @Test
    void cleanupExpiredHolds_noExpiredBookings_doesNotCallDeleteAll() {
        when(bookingRepository.findByStatusAndHeldAtBefore(eq(BookingStatus.HELD), any(Instant.class)))
                .thenReturn(List.of());

        holdCleanupService.cleanupExpiredHolds();

        verify(bookingRepository, never()).deleteAll(any(List.class));
    }
}
