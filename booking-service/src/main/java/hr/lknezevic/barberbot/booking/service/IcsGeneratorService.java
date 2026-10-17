package hr.lknezevic.barberbot.booking.service;

import hr.lknezevic.barberbot.booking.entity.Booking;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

@Slf4j
@Service
public class IcsGeneratorService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Zagreb");

    public byte[] generateIcs(Booking booking) {
        LocalDateTime start = LocalDateTime.of(booking.getDate(), booking.getTime());
        LocalDateTime end = start.plusMinutes(booking.getDurationMinutes());

        ZonedDateTime zonedStart = start.atZone(ZONE);
        ZonedDateTime zonedEnd = end.atZone(ZONE);

        String summary = booking.getService() + " kod " + booking.getBarber();

        VEvent event = new VEvent((Temporal) zonedStart, (Temporal) zonedEnd, summary);

        Calendar calendar = new Calendar();
        calendar.add(new ProdId("-//BarberBot//Booking//HR"));
        calendar.add(new Version(new ParameterList(), "2.0"));
        calendar.add(new CalScale("GREGORIAN"));
        calendar.add(event);

        String icsContent = calendar.toString();
        log.debug("Generated .ics for bookingId={}, size={} bytes", booking.getId(), icsContent.length());
        return icsContent.getBytes();
    }
}
