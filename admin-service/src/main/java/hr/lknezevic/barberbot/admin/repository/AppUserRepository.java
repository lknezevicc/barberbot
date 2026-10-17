package hr.lknezevic.barberbot.admin.repository;

import hr.lknezevic.barberbot.admin.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByTenantIdAndTelegramId(UUID tenantId, String telegramId);

    Optional<AppUser> findByEmail(String email);

    @Query("SELECT u FROM AppUser u JOIN FETCH u.tenant WHERE u.email = :email")
    Optional<AppUser> findByEmailWithTenant(@Param("email") String email);

    @Query("SELECT u FROM AppUser u JOIN FETCH u.tenant WHERE u.id = :id")
    Optional<AppUser> findByIdWithTenant(@Param("id") UUID id);

    boolean existsByEmail(String email);

    List<AppUser> findByTenantId(UUID tenantId);

    List<AppUser> findByTenantIdAndRole(UUID tenantId, String role);

    boolean existsByTenantIdAndTelegramId(UUID tenantId, String telegramId);
}
