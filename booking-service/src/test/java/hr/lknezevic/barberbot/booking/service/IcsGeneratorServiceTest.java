package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.Booking;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IcsGeneratorServiceTest {

    private final IcsGeneratorService icsGeneratorService = new IcsGeneratorService();

    @Test
    void generateIcs_producesValidIcsContent() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .service("Sisanje")
                .barber("Marko")
                .date(LocalDate.of(2026, 6, 1))
                .time(LocalTime.of(15, 0))
                .durationMinutes(30)
                .userId("user1")
                .chatId(100L)
                .build();

        byte[] icsBytes = icsGeneratorService.generateIcs(booking);

        assertThat(icsBytes).isNotEmpty();
        String icsContent = new String(icsBytes);
        assertThat(icsContent).contains("VCALENDAR");
        assertThat(icsContent).contains("VEVENT");
        assertThat(icsContent).contains("Sisanje kod Marko");
    }
}
