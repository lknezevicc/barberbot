package hr.lknezevic.barberbot.handler.update;

import hr.lknezevic.barberbot.enums.BarberBotCommand;
import hr.lknezevic.barberbot.enums.HandlerResult;
import hr.lknezevic.barberbot.enums.SessionState;
import hr.lknezevic.barberbot.model.UpdateHandlerContext;
import hr.lknezevic.barberbot.service.BotMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
@RequiredArgsConstructor
public class HelpCommandHandler implements UpdateHandler {

    private final BotMessageService botMessageService;

    @Override
    public EnumSet<SessionState> supportedStates() {
        return EnumSet.allOf(SessionState.class);
    }

    @Override
    public boolean supports(UpdateHandlerContext value) {
        return value.isCommand(BarberBotCommand.HELP);
    }

    @Override
    public HandlerResult handle(UpdateHandlerContext value) {
        String helpMessage = """
            Dobrodošli u BarberBot! 🧔✂️
            
            /start - Pokreni bota
            /rezerviraj - Rezerviraj termin
            /my_bookings - Pogledaj svoje termine
            /help - Prikaži ovu poruku
            /cancel - Prekini trenutnu rezervaciju
            """;

        botMessageService.sendMessage(value.chatId(), helpMessage);

        return HandlerResult.STOP;
    }
}
