package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.AppUser;
import hr.lknezevic.barberbot.booking.entity.Booking;
import hr.lknezevic.barberbot.booking.repository.AppUserRepository;
import hr.lknezevic.barberbot.booking.repository.BookingRepository;
import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.enums.BookingStatus;
import hr.lknezevic.barberbot.model.event.TelegramReplyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingReminderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingRepository bookingRepository;
    private final AppUserRepository appUserRepository;
    private final RabbitTemplate rabbitTemplate;

    private final Set<UUID> sentTomorrowReminders = ConcurrentHashMap.newKeySet();
    private final Set<UUID> sentSoonReminders = ConcurrentHashMap.newKeySet();
    private LocalDate lastCleanupDate = LocalDate.now();

    @Scheduled(fixedRate = 1_800_000)
    public void sendReminders() {
        cleanupStaleIds();

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Booking> tomorrowBookings = bookingRepository
                .findByDateAndStatus(tomorrow, BookingStatus.CONFIRMED);

        int tomorrowCount = 0;
        for (Booking booking : tomorrowBookings) {
            if (sentTomorrowReminders.contains(booking.getId())) continue;
            String text = "Podsjetnik: Sutra imaš termin " + booking.getService()
                    + " kod " + booking.getBarber()
                    + " u " + booking.getTime().format(TIME_FMT) + ". Vidimo se!";
            publishReminder(booking, text);
            sentTomorrowReminders.add(booking.getId());
            tomorrowCount++;
        }

        LocalTime now = LocalTime.now();
        LocalTime oneHourLater = now.plusHours(1);
        List<Booking> todayBookings = bookingRepository
                .findByDateAndStatus(LocalDate.now(), BookingStatus.CONFIRMED);

        int soonCount = 0;
        for (Booking booking : todayBookings) {
            if (sentSoonReminders.contains(booking.getId())) continue;
            if (booking.getTime().isAfter(now) && booking.getTime().isBefore(oneHourLater)) {
                String text = "Podsjetnik: Za manje od sat vremena imaš termin "
                        + booking.getService() + " kod " + booking.getBarber() + "!";
                publishReminder(booking, text);
                sentSoonReminders.add(booking.getId());
                soonCount++;
            }
        }

        if (tomorrowCount > 0 || soonCount > 0) {
            log.info("Sent {} tomorrow reminders and {} soon reminders", tomorrowCount, soonCount);
        }
    }

    private void cleanupStaleIds() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastCleanupDate)) {
            sentTomorrowReminders.clear();
            sentSoonReminders.clear();
            lastCleanupDate = today;
            log.debug("Cleared reminder tracking sets for new day");
        }
    }

    public void notifyAdminOnNewBooking(Booking booking) {
        UUID tenantId = booking.getTenant() != null ? booking.getTenant().getId() : null;
        if (tenantId == null) return;

        List<AppUser> admins = appUserRepository.findByTenantIdAndRole(tenantId, "ADMIN");
        String text = "Nova rezervacija! " + booking.getService()
                + " kod " + booking.getBarber()
                + " na " + booking.getDate().format(DATE_FMT)
                + " u " + booking.getTime().format(TIME_FMT);

        for (AppUser admin : admins) {
            if (admin.getChatId() == null || admin.getChatId() == 0L) {
                log.debug("Skipping admin notification for {} (no Telegram chatId)", admin.getId());
                continue;
            }
            TelegramReplyEvent event = TelegramReplyEvent.of(
                    tenantId.toString(), admin.getChatId(), text);
            rabbitTemplate.convertAndSend(RabbitQueues.EXCHANGE, RabbitQueues.REPLY_OUTBOUND_ROUTING_KEY, event);
            log.info("Notified admin {} about new booking", admin.getId());
        }
    }

    private void publishReminder(Booking booking, String text) {
        String tenantId = booking.getTenant() != null ? booking.getTenant().getId().toString() : null;
        TelegramReplyEvent event = TelegramReplyEvent.of(tenantId, booking.getChatId(), text);
        rabbitTemplate.convertAndSend(RabbitQueues.EXCHANGE, RabbitQueues.REPLY_OUTBOUND_ROUTING_KEY, event);
    }
}
