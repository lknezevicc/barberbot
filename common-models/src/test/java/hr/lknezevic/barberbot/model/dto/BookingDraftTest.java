package hr.lknezevic.barberbot.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class BookingDraftTest {

    @Test
    void isComplete_allFieldsPresent_returnsTrue() {
        BookingDraft draft = new BookingDraft("sisanje", LocalDate.of(2026, 6, 1), LocalTime.of(15, 0), "Marko");
        assertThat(draft.isComplete()).isTrue();
    }

    @Test
    void isComplete_missingService_returnsFalse() {
        BookingDraft draft = new BookingDraft(null, LocalDate.of(2026, 6, 1), LocalTime.of(15, 0), "Marko");
        assertThat(draft.isComplete()).isFalse();
    }

    @Test
    void isComplete_allNull_returnsFalse() {
        BookingDraft draft = new BookingDraft(null, null, null, null);
        assertThat(draft.isComplete()).isFalse();
    }

    @Test
    void missingFields_allPresent_returnsEmpty() {
        BookingDraft draft = new BookingDraft("sisanje", LocalDate.of(2026, 6, 1), LocalTime.of(15, 0), "Marko");
        assertThat(draft.missingFields()).isEmpty();
    }

    @Test
    void missingFields_someNull_returnsThoseNames() {
        BookingDraft draft = new BookingDraft("sisanje", null, null, "Marko");
        assertThat(draft.missingFields()).containsExactly("date", "time");
    }

    @Test
    void missingFields_allNull_returnsFour() {
        BookingDraft draft = new BookingDraft(null, null, null, null);
        assertThat(draft.missingFields()).containsExactly("service", "date", "time", "barber");
    }

    @Test
    void merge_incomingOverridesNonNullFields() {
        BookingDraft existing = new BookingDraft("sisanje", LocalDate.of(2026, 6, 1), null, null);
        BookingDraft incoming = new BookingDraft(null, null, LocalTime.of(15, 0), "Luka");

        BookingDraft merged = existing.merge(incoming);

        assertThat(merged.service()).isEqualTo("sisanje");
        assertThat(merged.date()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(merged.time()).isEqualTo(LocalTime.of(15, 0));
        assertThat(merged.barber()).isEqualTo("Luka");
    }

    @Test
    void merge_incomingOverridesExistingValues() {
        BookingDraft existing = new BookingDraft("sisanje", LocalDate.of(2026, 6, 1), LocalTime.of(10, 0), "Marko");
        BookingDraft incoming = new BookingDraft("brijanje", null, LocalTime.of(15, 0), null);

        BookingDraft merged = existing.merge(incoming);

        assertThat(merged.service()).isEqualTo("brijanje");
        assertThat(merged.date()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(merged.time()).isEqualTo(LocalTime.of(15, 0));
        assertThat(merged.barber()).isEqualTo("Marko");
    }

    @Test
    void merge_nullIncoming_returnsSelf() {
        BookingDraft existing = new BookingDraft("sisanje", null, null, null);
        assertThat(existing.merge(null)).isSameAs(existing);
    }
}
