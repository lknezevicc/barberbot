package hr.lknezevic.barberbot.booking.repository;

import hr.lknezevic.barberbot.booking.entity.WorkingHoursEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkingHoursRepository extends JpaRepository<WorkingHoursEntry, UUID> {

    List<WorkingHoursEntry> findByEmployeeId(UUID employeeId);

    List<WorkingHoursEntry> findByTenantId(UUID tenantId);

    Optional<WorkingHoursEntry> findByEmployeeIdAndDayOfWeek(UUID employeeId, int dayOfWeek);

    void deleteByEmployeeId(UUID employeeId);
}
