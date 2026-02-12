package hr.lknezevic.barberbot.model.event;

import java.time.Instant;

public record TelegramUpdateEvent(
        String tenantId,
        String updateJson,
        Instant receivedAt
) {

    public static TelegramUpdateEvent of(String updateJson) {
        return new TelegramUpdateEvent(null, updateJson, Instant.now());
    }

    public static TelegramUpdateEvent of(String tenantId, String updateJson) {
        return new TelegramUpdateEvent(tenantId, updateJson, Instant.now());
    }
}
