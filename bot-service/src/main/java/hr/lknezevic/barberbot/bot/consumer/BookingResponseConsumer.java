package hr.lknezevic.barberbot.bot.consumer;

import hr.lknezevic.barberbot.bot.publisher.ReplyPublisher;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.dto.InlineButton;
import hr.lknezevic.barberbot.model.event.AlternativeSlot;
import hr.lknezevic.barberbot.model.event.BookingCancelledEvent;
import hr.lknezevic.barberbot.model.event.BookingConfirmedEvent;
import hr.lknezevic.barberbot.model.event.BookingHeldEvent;
import hr.lknezevic.barberbot.model.event.BookingListRequestEvent;
import hr.lknezevic.barberbot.model.event.BookingListResponseEvent;
import hr.lknezevic.barberbot.model.event.BookingListResponseEvent.BookingSummary;
import hr.lknezevic.barberbot.model.event.BookingSlotUnavailableEvent;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingResponseConsumer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ReplyPublisher replyPublisher;

    @RabbitListener(queues = RabbitQueues.BOOKING_HELD_QUEUE)
    public void onBookingHeld(BookingHeldEvent event) {
        log.info("Slot held: bookingId={}, userId={}, barber={}, date={}, time={}",
                event.bookingId(), event.userId(), event.barber(), event.date(), event.time());

        String summary = event.service() + " kod " + event.barber()
                + "\n" + event.date().format(DATE_FMT) + " u " + event.time().format(TIME_FMT);

        List<InlineButton> buttons = List.of(
                new InlineButton("Potvrdi", "hold:confirm:" + event.bookingId()),
                new InlineButton("Odustani", "hold:cancel:" + event.bookingId())
        );

        replyPublisher.publish(
                TelegramReplyEvent.withButtons(null, event.chatId(), summary + "\n\nPotvrđuješ?", buttons));
    }

    @RabbitListener(queues = RabbitQueues.BOOKING_SLOT_UNAVAILABLE_QUEUE)
    public void onSlotUnavailable(BookingSlotUnavailableEvent event) {
        log.info("Slot unavailable for userId={}, barber={}, date={}, time={}, alternatives={}",
                event.userId(), event.barber(), event.originalDate(), event.originalTime(),
                event.alternatives().size());

        if (event.alternatives().isEmpty()) {
            replyPublisher.publish(TelegramReplyEvent.of(
                    event.chatId(),
                    "Nažalost, traženi termin nije dostupan i nema slobodnih alternativa u sljedećih 14 dana. "
                            + "Pokušaj s drugim datumom ili barberom."));
            return;
        }

        StringBuilder sb = new StringBuilder("Traženi termin (")
                .append(event.originalDate().format(DATE_FMT))
                .append(" u ").append(event.originalTime().format(TIME_FMT))
                .append(") je zauzet.\n\nSlobodni termini:");

        List<InlineButton> buttons = new ArrayList<>();
        for (AlternativeSlot alt : event.alternatives()) {
            String label = alt.date().format(DATE_FMT) + " u " + alt.time().format(TIME_FMT);
            String callbackData = "slot:" + alt.date() + ":" + alt.time();
            buttons.add(new InlineButton(label, callbackData));
            sb.append("\n• ").append(label);
        }

        replyPublisher.publish(
                TelegramReplyEvent.withButtons(null, event.chatId(), sb.toString(), buttons));
    }

    @RabbitListener(queues = RabbitQueues.BOOKING_CONFIRMED_QUEUE)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Booking confirmed: id={}, userId={}", event.bookingId(), event.userId());

        String text = "Termin potvrđen! " + event.service() + " kod " + event.barber()
                + " na " + event.date().format(DATE_FMT) + " u " + event.time().format(TIME_FMT)
                + ". Vidimo se!";

        replyPublisher.publish(TelegramReplyEvent.of(event.chatId(), text));
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
