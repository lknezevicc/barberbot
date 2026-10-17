package hr.lknezevic.barberbot.admin.repository;

import hr.lknezevic.barberbot.admin.entity.Booking;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByTenantIdOrderByDateDescTimeDesc(UUID tenantId);

    List<Booking> findByTenantIdAndDate(UUID tenantId, LocalDate date);

    List<Booking> findByTenantIdAndStatus(UUID tenantId, BookingStatus status);

    List<Booking> findByTenantIdAndDateAndStatus(UUID tenantId, LocalDate date, BookingStatus status);

    long countByTenantIdAndDateAndStatus(UUID tenantId, LocalDate date, BookingStatus status);

    long countByTenantIdAndStatus(UUID tenantId, BookingStatus status);

    List<Booking> findByUserIdAndStatusNotOrderByDateAscTimeAsc(String userId, BookingStatus excludedStatus);

    List<Booking> findByUserIdAndStatusAndDateGreaterThanEqualOrderByDateAscTimeAsc(
            String userId, BookingStatus status, LocalDate fromDate);

    List<Booking> findByBarberAndDateAndStatusIn(String barber, LocalDate date, List<BookingStatus> statuses);

    List<Booking> findByStatusAndHeldAtBefore(BookingStatus status, Instant cutoff);

    List<Booking> findTop5ByTenantIdAndStatusOrderByDateDescTimeDesc(UUID tenantId, BookingStatus status);

    List<Booking> findByTenantIdAndStatusAndDateGreaterThanEqual(UUID tenantId, BookingStatus status, LocalDate fromDate);

    List<Booking> findByTenantIdAndDateBetween(UUID tenantId, LocalDate from, LocalDate to);

    long countByTenantIdAndStatusAndDateBetween(UUID tenantId, BookingStatus status, LocalDate from, LocalDate to);

    @Query("SELECT b.barber FROM Booking b WHERE b.tenant.id = :tenantId AND b.status = 'CONFIRMED' GROUP BY b.barber ORDER BY COUNT(b) DESC LIMIT 1")
    Optional<String> findMostPopularBarber(@Param("tenantId") UUID tenantId);

    @Query("SELECT b.service FROM Booking b WHERE b.tenant.id = :tenantId AND b.status = 'CONFIRMED' GROUP BY b.service ORDER BY COUNT(b) DESC LIMIT 1")
    Optional<String> findMostPopularService(@Param("tenantId") UUID tenantId);
}
