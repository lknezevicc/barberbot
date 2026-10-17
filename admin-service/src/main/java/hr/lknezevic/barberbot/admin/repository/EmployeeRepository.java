package hr.lknezevic.barberbot.admin.repository;

import hr.lknezevic.barberbot.admin.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByTenantIdAndActiveTrue(UUID tenantId);

    List<Employee> findByTenantId(UUID tenantId);

    Optional<Employee> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
