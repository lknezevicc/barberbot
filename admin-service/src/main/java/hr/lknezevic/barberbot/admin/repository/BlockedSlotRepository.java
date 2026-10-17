package hr.lknezevic.barberbot.admin.repository;

import hr.lknezevic.barberbot.admin.entity.BlockedSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BlockedSlotRepository extends JpaRepository<BlockedSlot, UUID> {

    List<BlockedSlot> findByEmployeeIdAndDate(UUID employeeId, LocalDate date);

    List<BlockedSlot> findByTenantId(UUID tenantId);

    List<BlockedSlot> findByEmployeeIdAndDateBetween(UUID employeeId, LocalDate from, LocalDate to);

    List<BlockedSlot> findByTenantIdAndDateBetween(UUID tenantId, LocalDate from, LocalDate to);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BlockedSlot b " +
            "WHERE b.employee.id = :employeeId AND b.date = :date AND b.allDay = true")
    boolean isFullDayBlocked(UUID employeeId, LocalDate date);
}
