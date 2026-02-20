package hr.lknezevic.barberbot.booking.repository;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserIdAndStatusNotOrderByDateAscTimeAsc(String userId, BookingStatus excludedStatus);

    List<Booking> findByUserIdAndStatusAndDateGreaterThanEqualOrderByDateAscTimeAsc(
            String userId, BookingStatus status, LocalDate fromDate);
}
