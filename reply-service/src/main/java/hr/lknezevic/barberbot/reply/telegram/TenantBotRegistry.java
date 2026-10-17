package hr.lknezevic.barberbot.reply.telegram;

import hr.lknezevic.barberbot.model.dto.InlineButton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TenantBotRegistry {

    private final Map<String, DefaultAbsSender> senders = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbcTemplate;
    private final DefaultAbsSender defaultSender;

    public TenantBotRegistry(JdbcTemplate jdbcTemplate, TelegramGateway defaultSender) {
        this.jdbcTemplate = jdbcTemplate;
        this.defaultSender = defaultSender;
        refreshTenants();
    }

    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 60_000)
    public void refreshTenants() {
        try {
            jdbcTemplate.query(
                    "SELECT id::text, bot_token FROM tenants WHERE active = true",
                    rs -> {
                        String tenantId = rs.getString("id");
                        String botToken = rs.getString("bot_token");
                        String cachedToken = tokenCache.get(tenantId);
                        if (!botToken.equals(cachedToken)) {
                            senders.put(tenantId, createSender(botToken));
                            tokenCache.put(tenantId, botToken);
                            log.info("Registered/updated bot sender for tenant={}", tenantId);
                        }
                    }
            );
            log.debug("Refreshed tenant bot registry, {} tenants loaded", senders.size());
        } catch (Exception e) {
            log.warn("Failed to refresh tenant registry (tenants table may not exist yet): {}", e.getMessage());
        }
    }

    public void sendTextMessage(String tenantId, long chatId, String text,
                                String parseMode, List<InlineButton> buttons) throws TelegramApiException {
        DefaultAbsSender sender = resolveSender(tenantId);

        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(chatId)
                .text(text);

        if (parseMode != null) {
            builder.parseMode(parseMode);
        }

        if (buttons != null && !buttons.isEmpty()) {
            List<InlineKeyboardButton> row = buttons.stream()
                    .map(btn -> {
                        InlineKeyboardButton kb = new InlineKeyboardButton();
                        kb.setText(btn.text());
                        kb.setCallbackData(btn.callbackData());
                        return kb;
                    })
                    .toList();

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(List.of(row));
            builder.replyMarkup(markup);
        }

        sender.execute(builder.build());
    }

    public void sendDocument(String tenantId, long chatId, String text,
                             byte[] documentData, String filename) throws TelegramApiException {
        DefaultAbsSender sender = resolveSender(tenantId);

        InputFile inputFile = new InputFile(new ByteArrayInputStream(documentData), filename);
        SendDocument.SendDocumentBuilder builder = SendDocument.builder()
                .chatId(chatId)
                .document(inputFile);

        if (text != null && !text.isBlank()) {
            builder.caption(text);
        }

        sender.execute(builder.build());
    }

    private DefaultAbsSender resolveSender(String tenantId) {
        if (tenantId == null || tenantId.isBlank() || "default".equals(tenantId)) {
            return defaultSender;
        }
        DefaultAbsSender sender = senders.get(tenantId);
        if (sender == null) {
            log.warn("No bot sender found for tenant={}, using default", tenantId);
            return defaultSender;
        }
        return sender;
    }

    private DefaultAbsSender createSender(String botToken) {
        return new DefaultAbsSender(new DefaultBotOptions(), botToken) {};
    }
}
