package hr.lknezevic.barberbot.config.application;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "telegram")
public record TelegramConfig(
        @NotBlank String botName,
        @NotBlank String botUsername,
        @NotBlank String botToken,
        @NotBlank String secretToken,
        @NotBlank String actionTokenSecret
) {}
