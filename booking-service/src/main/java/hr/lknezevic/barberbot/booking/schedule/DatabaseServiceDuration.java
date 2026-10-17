package hr.lknezevic.barberbot.booking.schedule;

import hr.lknezevic.barberbot.booking.entity.ServiceEntity;
import hr.lknezevic.barberbot.booking.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DatabaseServiceDuration extends ServiceDuration {

    private final ServiceRepository serviceRepository;

    public Duration getDuration(UUID tenantId, String serviceName) {
        if (tenantId == null || serviceName == null) {
            return super.getDuration(serviceName);
        }

        Optional<ServiceEntity> service = serviceRepository
                .findByTenantIdAndNameIgnoreCase(tenantId, serviceName.trim());

        return service
                .map(s -> Duration.ofMinutes(s.getDurationMinutes()))
                .orElseGet(() -> super.getDuration(serviceName));
    }

    public int getDurationMinutes(UUID tenantId, String serviceName) {
        return (int) getDuration(tenantId, serviceName).toMinutes();
    }
}
