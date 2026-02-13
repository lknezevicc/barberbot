package hr.lknezevic.barberbot.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(
        @NotNull Telegram telegram
) {

    @Validated
    public record Telegram(
            @NotBlank String secretToken
    ) {
    }
}
