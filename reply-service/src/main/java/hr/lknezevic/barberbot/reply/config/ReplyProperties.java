package hr.lknezevic.barberbot.reply.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "reply")
public record ReplyProperties(
        @NotNull Telegram telegram
) {

    @Validated
    public record Telegram(
            @NotBlank String botName,
            @NotBlank String botUsername,
            @NotBlank String botToken
    ) {
    }
}
