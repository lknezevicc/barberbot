package hr.lknezevic.barberbot.booking.consumer;

import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.publisher.BookingEventPublisher;
import hr.lknezevic.barberbot.booking.schedule.ServiceDuration;
import hr.lknezevic.barberbot.booking.schedule.WorkingHoursProvider;
import hr.lknezevic.barberbot.booking.schedule.TimeRange;
import hr.lknezevic.barberbot.booking.service.BookingService;
import hr.lknezevic.barberbot.booking.service.SlotAvailabilityService;
import hr.lknezevic.barberbot.booking.service.SlotAvailabilityService.AvailableSlot;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.AlternativeSlot;
import hr.lknezevic.barberbot.model.event.BookingHeldEvent;
import hr.lknezevic.barberbot.model.event.BookingRequestEvent;
import hr.lknezevic.barberbot.model.event.BookingSlotUnavailableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingRequestConsumer {

    private static final int ALTERNATIVE_SLOT_COUNT = 3;

    private final BookingService bookingService;
    private final BookingEventPublisher eventPublisher;
    private final SlotAvailabilityService slotAvailabilityService;
    private final WorkingHoursProvider workingHoursProvider;
    private final ServiceDuration serviceDuration;

    @RabbitListener(queues = RabbitQueues.BOOKING_REQUEST_QUEUE)
    public void onBookingRequest(BookingRequestEvent event) {
        log.info("Received booking request: userId={}, service={}, date={}, time={}, barber={}",
                event.userId(), event.service(), event.date(), event.time(), event.barber());

        if (event.date().isBefore(LocalDate.now())) {
            log.warn("Rejected booking request for past date: {}", event.date());
            publishUnavailable(event, List.of());
            return;
        }

        Optional<TimeRange> hours = workingHoursProvider.getWorkingHours(event.date().getDayOfWeek());
        if (hours.isEmpty()) {
            log.warn("Rejected booking request for non-working day: {}", event.date());
            publishUnavailableWithAlternatives(event);
            return;
        }

        Duration duration = serviceDuration.getDuration(event.service());
        LocalTime slotEnd = event.time().plus(duration);

        if (!hours.get().fitsEntirely(event.time(), slotEnd)) {
            log.warn("Rejected booking request: time {} doesn't fit working hours {}", event.time(), hours.get());
            publishUnavailableWithAlternatives(event);
            return;
        }

        boolean available = slotAvailabilityService.isSlotAvailable(
                event.barber(), event.date(), event.time(), duration);

        if (available) {
            Booking held = bookingService.holdSlot(
                    event.userId(), event.chatId(), event.service(),
                    event.date(), event.time(), event.barber(),
                    serviceDuration.getDurationMinutes(event.service()));

            log.info("Slot held: bookingId={}, barber={}, date={}, time={}",
                    held.getId(), held.getBarber(), held.getDate(), held.getTime());

            eventPublisher.publishHeld(new BookingHeldEvent(
                    held.getId(), held.getUserId(), held.getChatId(),
                    held.getService(), held.getDate(), held.getTime(), held.getBarber()));
        } else {
            log.info("Slot unavailable: barber={}, date={}, time={}", event.barber(), event.date(), event.time());
            publishUnavailableWithAlternatives(event);
        }
    }

    private void publishUnavailableWithAlternatives(BookingRequestEvent event) {
        List<AvailableSlot> alternatives = slotAvailabilityService.findNextAvailableSlots(
                event.barber(), event.date(), event.time(), event.service(), ALTERNATIVE_SLOT_COUNT);

        List<AlternativeSlot> altSlots = alternatives.stream()
                .map(s -> new AlternativeSlot(s.date(), s.time()))
                .toList();

        publishUnavailable(event, altSlots);
    }

    private void publishUnavailable(BookingRequestEvent event, List<AlternativeSlot> alternatives) {
        eventPublisher.publishSlotUnavailable(new BookingSlotUnavailableEvent(
                event.userId(), event.chatId(), event.service(),
                event.date(), event.time(), event.barber(), alternatives));
    }
}
