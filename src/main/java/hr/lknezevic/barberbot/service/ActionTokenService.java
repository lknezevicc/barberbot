package hr.lknezevic.barberbot.service;

import hr.lknezevic.barberbot.config.application.TelegramConfig;
import hr.lknezevic.barberbot.enums.BarberBotCommand;
import hr.lknezevic.barberbot.model.ActionToken;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class ActionTokenService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final TelegramConfig telegramConfig;

    public String encode(BarberBotCommand command, String bookingId) {
        String payload = command.name() + "|" + bookingId + "|" + LocalDateTime.now().format(formatter);
        String signature = sign(payload);

        String data = payload + "|" + signature;
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    public ActionToken decode(String data) {
        String decoded = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\|");

        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid action token!");
        }

        String payload = String.join("|", parts[0], parts[1], parts[2]);
        String signature = parts[3];

        if (!verify(payload, signature)) {
            throw new SecurityException("Invalid token signature!");
        }

        return new ActionToken(
                BarberBotCommand.valueOf(parts[0]),
                parts[1],
                LocalDateTime.parse(parts[2], formatter)
        );
    }

    private String sign(String payload) {
        byte[] raw = HmacUtils
                .getInitializedMac(
                        HmacAlgorithms.HMAC_SHA_256,
                        telegramConfig.actionTokenSecret().getBytes()
                ).doFinal(payload.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(raw);
    }

    private boolean verify(String payload, String signature) {
        return sign(payload).equals(signature);
    }
}
