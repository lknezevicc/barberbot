package hr.lknezevic.barberbot.bot.consumer;

import hr.lknezevic.barberbot.bot.publisher.ReplyPublisher;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.BookingCancelledEvent;
import hr.lknezevic.barberbot.model.event.BookingConfirmedEvent;
import hr.lknezevic.barberbot.model.event.BookingListResponseEvent;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

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
        log.info("Booking list response for userId={}, count={}", event.userId(), event.bookings().size());

        String text;
        if (event.bookings().isEmpty()) {
            text = "Nemaš nijedan nadolazeći termin.";
        } else {
            StringBuilder sb = new StringBuilder("Tvoji nadolazeći termini:\n\n");
            for (int i = 0; i < event.bookings().size(); i++) {
                var b = event.bookings().get(i);
                sb.append(i + 1).append(". ")
                        .append(b.service()).append(" kod ").append(b.barber())
                        .append("\n   ").append(b.date().format(DATE_FMT))
                        .append(" u ").append(b.time().format(TIME_FMT))
                        .append("\n");
            }
            text = sb.toString().trim();
        }

        replyPublisher.publish(TelegramReplyEvent.of(event.chatId(), text));
    }

    @RabbitListener(queues = RabbitQueues.BOOKING_CANCELLED_QUEUE)
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("Booking cancel response: id={}, success={}", event.bookingId(), event.success());
        replyPublisher.publish(TelegramReplyEvent.of(event.chatId(), event.message()));
    }
}
