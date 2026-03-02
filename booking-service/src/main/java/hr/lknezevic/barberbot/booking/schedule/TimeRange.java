package hr.lknezevic.barberbot.booking.schedule;

import java.time.LocalTime;

public record TimeRange(LocalTime start, LocalTime end) {

    public boolean contains(LocalTime time) {
        return !time.isBefore(start) && time.isBefore(end);
    }

    public boolean fitsEntirely(LocalTime slotStart, LocalTime slotEnd) {
        return !slotStart.isBefore(start) && !slotEnd.isAfter(end);
    }
}
