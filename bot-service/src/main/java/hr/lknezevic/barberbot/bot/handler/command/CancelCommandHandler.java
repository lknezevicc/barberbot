package hr.lknezevic.barberbot.bot.handler.command;

import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.BotCommand;
import org.springframework.stereotype.Component;

@Component
public class CancelCommandHandler implements CommandHandler {

    @Override
    public BotCommand supportedCommand() {
        return BotCommand.CANCEL;
    }

    @Override
    public ReplyAction handle(InboundMessage message, Session session) {
        // TODO: integrate with booking-service when it's implemented
        return ReplyAction.of(
                message.chatId(),
                "Otkazivanje termina još nije dostupno. Uskoro!"
        );
    }
}
