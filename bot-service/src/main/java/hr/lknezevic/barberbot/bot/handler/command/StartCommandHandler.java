package hr.lknezevic.barberbot.bot.handler.command;

import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.BotCommand;
import org.springframework.stereotype.Component;

@Component
public class StartCommandHandler implements CommandHandler {

    @Override
    public BotCommand supportedCommand() {
        return BotCommand.START;
    }

    @Override
    public ReplyAction handle(InboundMessage message, Session session) {
        return ReplyAction.of(
                message.chatId(),
                "Pozdrav, ja sam BarberBot! \u2702\uFE0F Kako ti mogu pomoći?"
        );
    }
}
