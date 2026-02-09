package hr.lknezevic.barberbot.model;

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

    public List<String> emptyFields() {
        List<String> emptyFields = new ArrayList<>();

        if (service == null) emptyFields.add("service");
        if (date == null) emptyFields.add("date");
        if (time == null) emptyFields.add("time");
        if (barber == null) emptyFields.add("barber");

        return emptyFields;
    }

    @JsonIgnore
    public boolean isComplete() {
        return service != null &&
                date != null &&
                time != null &&
                barber != null;
    }

    public BookingDraft merge(BookingDraft incoming) {
        if (incoming == null || incoming.isComplete()) return this;
        return new BookingDraft(
                incoming.service() != null ? incoming.service() : this.service(),
                incoming.date() != null ? incoming.date() : this.date(),
                incoming.time() != null ? incoming.time() : this.time(),
                incoming.barber() != null ? incoming.barber() : this.barber()
        );
    }

}
