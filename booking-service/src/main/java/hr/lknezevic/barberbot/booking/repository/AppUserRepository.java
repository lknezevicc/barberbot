package hr.lknezevic.barberbot.booking.repository;

import hr.lknezevic.barberbot.booking.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByTenantIdAndTelegramId(UUID tenantId, String telegramId);

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AppUser> findByTenantId(UUID tenantId);

    List<AppUser> findByTenantIdAndRole(UUID tenantId, String role);

    boolean existsByTenantIdAndTelegramId(UUID tenantId, String telegramId);
}
