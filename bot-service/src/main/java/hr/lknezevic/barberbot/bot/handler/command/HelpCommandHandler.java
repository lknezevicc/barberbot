package hr.lknezevic.barberbot.bot.handler.command;

import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.BotCommand;
import org.springframework.stereotype.Component;

@Component
public class HelpCommandHandler implements CommandHandler {

    @Override
    public BotCommand supportedCommand() {
        return BotCommand.HELP;
    }

    @Override
    public ReplyAction handle(InboundMessage message, Session session) {
        String help = """
                Evo što mogu:
                /start - Pokretanje bota
                /my_bookings - Pregled tvojih termina
                /cancel - Otkaži termin
                /help - Ova poruka
                
                Ili mi jednostavno napiši što trebaš!""";

        return ReplyAction.of(message.chatId(), help);
    }
}
