package hr.lknezevic.barberbot.booking.publisher;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.BookingCancelledEvent;
import hr.lknezevic.barberbot.model.event.BookingConfirmedEvent;
import hr.lknezevic.barberbot.model.event.BookingListResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishConfirmed(BookingConfirmedEvent event) {
        log.debug("Publishing BookingConfirmedEvent for bookingId={}", event.bookingId());
        rabbitTemplate.convertAndSend(RabbitQueues.EXCHANGE, RabbitQueues.BOOKING_CONFIRMED_ROUTING_KEY, event);
    }

    public void publishListResponse(BookingListResponseEvent event) {
        log.debug("Publishing BookingListResponseEvent for userId={}", event.userId());
        rabbitTemplate.convertAndSend(RabbitQueues.EXCHANGE, RabbitQueues.BOOKING_LIST_RESPONSE_ROUTING_KEY, event);
    }

    public void publishCancelled(BookingCancelledEvent event) {
        log.debug("Publishing BookingCancelledEvent for bookingId={}", event.bookingId());
        rabbitTemplate.convertAndSend(RabbitQueues.EXCHANGE, RabbitQueues.BOOKING_CANCELLED_ROUTING_KEY, event);
    }
}
