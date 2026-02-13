package hr.lknezevic.barberbot.bot.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.enums.BotCommand;
import hr.lknezevic.barberbot.model.enums.MessageType;
import hr.lknezevic.barberbot.model.event.TelegramUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateEventParser {

    private final ObjectMapper objectMapper;

    public Optional<InboundMessage> parse(TelegramUpdateEvent event) {
        try {
            JsonNode root = objectMapper.readTree(event.updateJson());

            if (root.has("callback_query")) {
                return parseCallback(event.tenantId(), root.get("callback_query"));
            }

            if (root.has("message")) {
                return parseMessage(event.tenantId(), root.get("message"));
            }

            log.warn("Unsupported update type, skipping");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to parse TelegramUpdateEvent", e);
            return Optional.empty();
        }
    }

    private Optional<InboundMessage> parseMessage(String tenantId, JsonNode message) {
        Long chatId = message.path("chat").path("id").asLong();
        String userId = String.valueOf(message.path("from").path("id").asLong());
        String text = message.has("text") ? message.get("text").asText() : null;

        if (text == null) {
            log.debug("Message without text from userId={}, skipping", userId);
            return Optional.empty();
        }

        MessageType type = BotCommand.fromText(text).isPresent()
                ? MessageType.COMMAND
                : MessageType.TEXT;

        return Optional.of(new InboundMessage(tenantId, chatId, userId, text, null, type));
    }

    private Optional<InboundMessage> parseCallback(String tenantId, JsonNode callbackQuery) {
        Long chatId = callbackQuery.path("message").path("chat").path("id").asLong();
        String userId = String.valueOf(callbackQuery.path("from").path("id").asLong());
        String data = callbackQuery.has("data") ? callbackQuery.get("data").asText() : null;

        return Optional.of(new InboundMessage(tenantId, chatId, userId, null, data, MessageType.CALLBACK));
    }
}
