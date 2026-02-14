package hr.lknezevic.barberbot.bot.handler.command;

import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.BotCommand;

public interface CommandHandler {

    BotCommand supportedCommand();

    ReplyAction handle(InboundMessage message, Session session);
}
