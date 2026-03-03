package hr.lknezevic.barberbot.booking.consumer;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.publisher.BookingEventPublisher;
import hr.lknezevic.barberbot.booking.service.BookingService;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.BookingConfirmHoldEvent;
import hr.lknezevic.barberbot.model.event.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConfirmHoldConsumer {

    private final BookingService bookingService;
    private final BookingEventPublisher eventPublisher;

    @RabbitListener(queues = RabbitQueues.BOOKING_CONFIRM_HOLD_QUEUE)
    public void onConfirmHold(BookingConfirmHoldEvent event) {
        log.info("Received hold confirmation: bookingId={}, userId={}", event.bookingId(), event.userId());

        Optional<Booking> confirmed = bookingService.confirmHold(event.bookingId(), event.userId());

        confirmed.ifPresentOrElse(
                booking -> {
                    log.info("Hold confirmed: bookingId={}", booking.getId());
                    eventPublisher.publishConfirmed(new BookingConfirmedEvent(
                            booking.getId(), booking.getUserId(), booking.getChatId(),
                            booking.getService(), booking.getDate(), booking.getTime(),
                            booking.getBarber()));
                },
                () -> log.warn("Hold confirmation failed: bookingId={} (expired or not found)", event.bookingId())
        );
    }
}
