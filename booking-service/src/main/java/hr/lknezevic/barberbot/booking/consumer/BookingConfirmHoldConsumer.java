package hr.lknezevic.barberbot.booking.consumer;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.publisher.BookingEventPublisher;
import hr.lknezevic.barberbot.booking.service.BookingReminderService;
import hr.lknezevic.barberbot.booking.service.BookingService;
import hr.lknezevic.barberbot.booking.service.IcsGeneratorService;
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
    private final IcsGeneratorService icsGeneratorService;
    private final BookingReminderService bookingReminderService;

    @RabbitListener(queues = RabbitQueues.BOOKING_CONFIRM_HOLD_QUEUE)
    public void onConfirmHold(BookingConfirmHoldEvent event) {
        log.info("Received hold confirmation: bookingId={}, userId={}", event.bookingId(), event.userId());

        Optional<Booking> confirmed = bookingService.confirmHold(event.bookingId(), event.userId());

        confirmed.ifPresentOrElse(
                booking -> {
                    log.info("Hold confirmed: bookingId={}", booking.getId());

                    byte[] icsData = null;
                    try {
                        icsData = icsGeneratorService.generateIcs(booking);
                    } catch (Exception e) {
                        log.warn("Failed to generate .ics for bookingId={}: {}", booking.getId(), e.getMessage());
                    }

                    eventPublisher.publishConfirmed(new BookingConfirmedEvent(
                            booking.getId(), booking.getUserId(), booking.getChatId(),
                            event.tenantId(),
                            booking.getService(), booking.getDate(), booking.getTime(),
                            booking.getBarber(), icsData));

                    try {
                        bookingReminderService.notifyAdminOnNewBooking(booking);
                    } catch (Exception e) {
                        log.warn("Failed to notify admin for bookingId={}: {}", booking.getId(), e.getMessage());
                    }
                },
                () -> log.warn("Hold confirmation failed: bookingId={} (expired or not found)", event.bookingId())
        );
    }
}
