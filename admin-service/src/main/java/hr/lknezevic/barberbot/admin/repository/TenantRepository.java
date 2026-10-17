package hr.lknezevic.barberbot.admin.repository;

import hr.lknezevic.barberbot.admin.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySecretToken(String secretToken);

    Optional<Tenant> findByBotToken(String botToken);

    Optional<Tenant> findByName(String name);

    boolean existsByName(String name);

    boolean existsByBotToken(String botToken);
}
