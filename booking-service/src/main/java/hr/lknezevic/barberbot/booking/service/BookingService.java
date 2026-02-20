package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.repository.BookingRepository;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    @Transactional
    public Booking createBooking(Booking booking) {
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: id={}, userId={}, service={}, date={}, time={}",
                saved.getId(), saved.getUserId(), saved.getService(), saved.getDate(), saved.getTime());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Booking> getUpcomingBookings(String userId) {
        return bookingRepository.findByUserIdAndStatusAndDateGreaterThanEqualOrderByDateAscTimeAsc(
                userId, BookingStatus.CONFIRMED, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Booking> getActiveBookings(String userId) {
        return bookingRepository.findByUserIdAndStatusNotOrderByDateAscTimeAsc(userId, BookingStatus.CANCELLED);
    }

    @Transactional
    public Optional<Booking> cancelBooking(UUID bookingId, String userId) {
        return bookingRepository.findById(bookingId)
                .filter(b -> b.getUserId().equals(userId))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(booking -> {
                    booking.setStatus(BookingStatus.CANCELLED);
                    Booking saved = bookingRepository.save(booking);
                    log.info("Booking cancelled: id={}, userId={}", bookingId, userId);
                    return saved;
                });
    }
}
