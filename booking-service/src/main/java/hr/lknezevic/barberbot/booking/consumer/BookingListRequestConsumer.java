package hr.lknezevic.barberbot.booking.consumer;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.publisher.BookingEventPublisher;
import hr.lknezevic.barberbot.booking.service.BookingService;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.BookingListRequestEvent;
import hr.lknezevic.barberbot.model.event.BookingListResponseEvent;
import hr.lknezevic.barberbot.model.event.BookingListResponseEvent.BookingSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingListRequestConsumer {

    private final BookingService bookingService;
    private final BookingEventPublisher eventPublisher;

    @RabbitListener(queues = RabbitQueues.BOOKING_LIST_REQUEST_QUEUE)
    public void onListRequest(BookingListRequestEvent event) {
        log.info("Received booking list request for userId={}, purpose={}", event.userId(), event.purpose());

        List<Booking> bookings = bookingService.getUpcomingBookings(event.userId());

        List<BookingSummary> summaries = bookings.stream()
                .map(b -> new BookingSummary(
                        b.getId(), b.getService(), b.getDate(),
                        b.getTime(), b.getBarber(), b.getStatus().name()))
                .toList();

        eventPublisher.publishListResponse(
                new BookingListResponseEvent(event.userId(), event.chatId(), event.purpose(), summaries));
    }
}
