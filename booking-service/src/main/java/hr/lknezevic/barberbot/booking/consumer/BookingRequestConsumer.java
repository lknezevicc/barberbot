package hr.lknezevic.barberbot.booking.consumer;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.publisher.BookingEventPublisher;
import hr.lknezevic.barberbot.booking.service.BookingService;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.BookingConfirmedEvent;
import hr.lknezevic.barberbot.model.event.BookingRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingRequestConsumer {

    private final BookingService bookingService;
    private final BookingEventPublisher eventPublisher;

    @RabbitListener(queues = RabbitQueues.BOOKING_REQUEST_QUEUE)
    public void onBookingRequest(BookingRequestEvent event) {
        log.info("Received booking request for userId={}, service={}", event.userId(), event.service());

        Booking booking = Booking.builder()
                .userId(event.userId())
                .chatId(event.chatId())
                .service(event.service())
                .date(event.date())
                .time(event.time())
                .barber(event.barber())
                .build();

        Booking saved = bookingService.createBooking(booking);

        eventPublisher.publishConfirmed(new BookingConfirmedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getChatId(),
                saved.getService(),
                saved.getDate(),
                saved.getTime(),
                saved.getBarber()
        ));
    }
}
