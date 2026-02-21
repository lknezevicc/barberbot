package hr.lknezevic.barberbot.bot.service;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.BookingCancelRequestEvent;
import hr.lknezevic.barberbot.model.event.BookingListRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingQueryService {

    private final RabbitTemplate rabbitTemplate;

    public void requestBookingList(String userId, Long chatId) {
        rabbitTemplate.convertAndSend(
                RabbitQueues.EXCHANGE,
                RabbitQueues.BOOKING_LIST_REQUEST_ROUTING_KEY,
                BookingListRequestEvent.forView(userId, chatId));
        log.debug("Published booking list request (VIEW) for userId={}", userId);
    }

    public void requestBookingListForCancel(String userId, Long chatId) {
        rabbitTemplate.convertAndSend(
                RabbitQueues.EXCHANGE,
                RabbitQueues.BOOKING_LIST_REQUEST_ROUTING_KEY,
                BookingListRequestEvent.forCancel(userId, chatId));
        log.debug("Published booking list request (CANCEL) for userId={}", userId);
    }

    public void requestBookingCancel(UUID bookingId, String userId, Long chatId) {
        rabbitTemplate.convertAndSend(
                RabbitQueues.EXCHANGE,
                RabbitQueues.BOOKING_CANCEL_REQUEST_ROUTING_KEY,
                new BookingCancelRequestEvent(bookingId, userId, chatId));
        log.debug("Published booking cancel request for bookingId={}", bookingId);
    }
}
