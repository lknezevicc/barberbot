package hr.lknezevic.barberbot.service;

import hr.lknezevic.barberbot.telegram.TelegramGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public final class BotMessageService {

    private final TelegramGateway telegramGateway;

    public void handleMessage(Message message) {
        long chatId = message.getChatId();
        String text = message.getText();

        String reply = switch (text.toLowerCase().trim()) {
            case "/start" -> "Bok! Napiši 'rezerviraj' da dogovorimo termin.";
            case "rezerviraj" -> "Super — napiši datum i vrijeme (npr. 2026-01-20 14:30).";
            default -> "Nisam razumio. Napiši 'rezerviraj' ili /start.";
        };

        try {
            telegramGateway.sendMessage(chatId, reply);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId={}", chatId, e);
        }
    }
}
