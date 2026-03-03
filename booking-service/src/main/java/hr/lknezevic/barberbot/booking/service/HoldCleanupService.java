package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.repository.BookingRepository;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldCleanupService {

    private static final long HOLD_TTL_MINUTES = 2;

    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void cleanupExpiredHolds() {
        Instant cutoff = Instant.now().minus(HOLD_TTL_MINUTES, ChronoUnit.MINUTES);
        List<Booking> expired = bookingRepository.findByStatusAndHeldAtBefore(BookingStatus.HELD, cutoff);

        if (!expired.isEmpty()) {
            log.info("Cleaning up {} expired held bookings", expired.size());
            bookingRepository.deleteAll(expired);
        }
    }
}
