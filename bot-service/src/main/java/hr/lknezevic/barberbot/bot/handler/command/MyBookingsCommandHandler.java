package hr.lknezevic.barberbot.bot.handler.command;

import hr.lknezevic.barberbot.bot.service.BookingQueryService;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.BotCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MyBookingsCommandHandler implements CommandHandler {

    private final BookingQueryService bookingQueryService;

    @Override
    public BotCommand supportedCommand() {
        return BotCommand.MY_BOOKINGS;
    }

    @Override
    public ReplyAction handle(InboundMessage message, Session session) {
        bookingQueryService.requestBookingList(message.userId(), message.chatId());
        return ReplyAction.of(message.chatId(), "Dohvaćam tvoje termine...");
    }
}
