package hr.lknezevic.barberbot.config.application;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record ApplicationConfig(
        @NotNull Boolean enabled,
        @NotNull WorkerConfig worker
) {}
