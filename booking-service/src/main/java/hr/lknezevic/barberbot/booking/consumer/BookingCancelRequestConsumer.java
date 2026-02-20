package hr.lknezevic.barberbot.booking.consumer;

import hr.lknezevic.barberbot.booking.publisher.BookingEventPublisher;
import hr.lknezevic.barberbot.booking.service.BookingService;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.BookingCancelRequestEvent;
import hr.lknezevic.barberbot.model.event.BookingCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCancelRequestConsumer {

    private final BookingService bookingService;
    private final BookingEventPublisher eventPublisher;

    @RabbitListener(queues = RabbitQueues.BOOKING_CANCEL_REQUEST_QUEUE)
    public void onCancelRequest(BookingCancelRequestEvent event) {
        log.info("Received cancel request for bookingId={}, userId={}", event.bookingId(), event.userId());

        bookingService.cancelBooking(event.bookingId(), event.userId())
                .ifPresentOrElse(
                        booking -> eventPublisher.publishCancelled(new BookingCancelledEvent(
                                booking.getId(), event.userId(), event.chatId(),
                                true, "Termin je uspješno otkazan.")),
                        () -> eventPublisher.publishCancelled(new BookingCancelledEvent(
                                event.bookingId(), event.userId(), event.chatId(),
                                false, "Termin nije pronađen ili je već otkazan."))
                );
    }
}
