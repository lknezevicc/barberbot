package hr.lknezevic.barberbot.bot.consumer;

import hr.lknezevic.barberbot.bot.publisher.ReplyPublisher;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.dto.InlineButton;
import hr.lknezevic.barberbot.model.event.BookingCancelledEvent;
import hr.lknezevic.barberbot.model.event.BookingConfirmedEvent;
import hr.lknezevic.barberbot.model.event.BookingListRequestEvent;
import hr.lknezevic.barberbot.model.event.BookingListResponseEvent;
import hr.lknezevic.barberbot.model.event.BookingListResponseEvent.BookingSummary;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingResponseConsumer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ReplyPublisher replyPublisher;

    @RabbitListener(queues = RabbitQueues.BOOKING_CONFIRMED_QUEUE)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Booking confirmed: id={}, userId={}", event.bookingId(), event.userId());
    }

    @RabbitListener(queues = RabbitQueues.BOOKING_LIST_RESPONSE_QUEUE)
    public void onBookingListResponse(BookingListResponseEvent event) {
        log.info("Booking list response for userId={}, purpose={}, count={}",
                event.userId(), event.purpose(), event.bookings().size());

        if (BookingListRequestEvent.PURPOSE_CANCEL.equals(event.purpose())) {
            handleCancelList(event);
        } else {
            handleViewList(event);
        }
    }

    private void handleViewList(BookingListResponseEvent event) {
        if (event.bookings().isEmpty()) {
            replyPublisher.publish(TelegramReplyEvent.of(event.chatId(), "Nemaš nijedan nadolazeći termin."));
            return;
        }

        StringBuilder sb = new StringBuilder("Tvoji nadolazeći termini:\n\n");
        for (int i = 0; i < event.bookings().size(); i++) {
            BookingSummary b = event.bookings().get(i);
            sb.append(i + 1).append(". ")
                    .append(b.service()).append(" kod ").append(b.barber())
                    .append("\n   ").append(b.date().format(DATE_FMT))
                    .append(" u ").append(b.time().format(TIME_FMT))
                    .append("\n");
        }

        replyPublisher.publish(TelegramReplyEvent.of(event.chatId(), sb.toString().trim()));
    }

    private void handleCancelList(BookingListResponseEvent event) {
        if (event.bookings().isEmpty()) {
            replyPublisher.publish(TelegramReplyEvent.of(event.chatId(), "Nemaš nijedan termin za otkazivanje."));
            return;
        }

        StringBuilder sb = new StringBuilder("Odaberi termin koji želiš otkazati:\n\n");
        List<InlineButton> buttons = event.bookings().stream()
                .map(b -> {
                    String label = b.service() + " - " + b.date().format(DATE_FMT) + " " + b.time().format(TIME_FMT);
                    sb.append("• ").append(label).append(" kod ").append(b.barber()).append("\n");
                    return new InlineButton(label, "cancel:" + b.id());
                })
                .toList();

        replyPublisher.publish(
                TelegramReplyEvent.withButtons(null, event.chatId(), sb.toString().trim(), buttons));
    }

    @RabbitListener(queues = RabbitQueues.BOOKING_CANCELLED_QUEUE)
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("Booking cancel response: id={}, success={}", event.bookingId(), event.success());
        replyPublisher.publish(TelegramReplyEvent.of(event.chatId(), event.message()));
    }
}
