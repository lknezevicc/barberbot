package hr.lknezevic.barberbot.admin.repository;

import hr.lknezevic.barberbot.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndEntityTypeOrderByCreatedAtDesc(
            UUID tenantId, String entityType, Pageable pageable);

    Page<AuditLog> findByTenantIdAndActorIdOrderByCreatedAtDesc(
            UUID tenantId, UUID actorId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenant.id = :tenantId " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:startDate IS NULL OR a.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);
}
