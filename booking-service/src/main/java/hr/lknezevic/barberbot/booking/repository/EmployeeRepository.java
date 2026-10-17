package hr.lknezevic.barberbot.booking.repository;

import hr.lknezevic.barberbot.booking.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByTenantIdAndActiveTrue(UUID tenantId);

    List<Employee> findByTenantId(UUID tenantId);

    Optional<Employee> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    @Query("SELECT e FROM Employee e WHERE e.tenant.id = :tenantId AND e.active = true " +
           "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :partialName, '%'))")
    List<Employee> findByTenantIdAndNameContainingIgnoreCase(UUID tenantId, String partialName);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
