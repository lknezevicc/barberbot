package hr.lknezevic.barberbot.admin.repository;

import hr.lknezevic.barberbot.admin.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {

    List<ServiceEntity> findByTenantIdAndActiveTrue(UUID tenantId);

    List<ServiceEntity> findByTenantId(UUID tenantId);

    Optional<ServiceEntity> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
