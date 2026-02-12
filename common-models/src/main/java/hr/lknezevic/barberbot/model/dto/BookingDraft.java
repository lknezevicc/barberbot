package hr.lknezevic.barberbot.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public record BookingDraft(
        String service,
        LocalDate date,
        LocalTime time,
        String barber
) {

    public List<String> missingFields() {
        List<String> missing = new ArrayList<>();
        if (service == null) missing.add("service");
        if (date == null) missing.add("date");
        if (time == null) missing.add("time");
        if (barber == null) missing.add("barber");
        return missing;
    }

    @JsonIgnore
    public boolean isComplete() {
        return service != null && date != null && time != null && barber != null;
    }

    public BookingDraft merge(BookingDraft incoming) {
        if (incoming == null) return this;
        return new BookingDraft(
                incoming.service() != null ? incoming.service() : this.service,
                incoming.date() != null ? incoming.date() : this.date,
                incoming.time() != null ? incoming.time() : this.time,
                incoming.barber() != null ? incoming.barber() : this.barber
        );
    }
}
