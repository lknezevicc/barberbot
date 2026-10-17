package hr.lknezevic.barberbot.model.dto;

import hr.lknezevic.barberbot.model.enums.SessionState;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 10, 0);

    private Session baseSession() {
        return new Session("sess-1", "user-42", SessionState.IDLE, null, NOW, NOW);
    }

    @Test
    void withState_returnsNewSessionWithUpdatedState() {
        Session original = baseSession();
        Session updated = original.withState(SessionState.CONFIRMING);

        assertThat(updated.state()).isEqualTo(SessionState.CONFIRMING);
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.userId()).isEqualTo(original.userId());
        assertThat(updated.bookingDraft()).isNull();
    }

    @Test
    void withState_doesNotMutateOriginal() {
        Session original = baseSession();
        original.withState(SessionState.CONFIRMING);

        assertThat(original.state()).isEqualTo(SessionState.IDLE);
    }

    @Test
    void withBookingDraft_setsNewDraft() {
        Session original = baseSession();
        BookingDraft draft = new BookingDraft("sisanje", LocalDate.of(2026, 6, 1), LocalTime.of(15, 0), "Marko");

        Session updated = original.withBookingDraft(draft);

        assertThat(updated.bookingDraft()).isEqualTo(draft);
        assertThat(updated.state()).isEqualTo(SessionState.IDLE);
    }

    @Test
    void withBookingDraft_clearsDraft() {
        BookingDraft draft = new BookingDraft("sisanje", null, null, null);
        Session withDraft = new Session("sess-1", "user-42", SessionState.CONFIRMING, draft, NOW, NOW);

        Session cleared = withDraft.withBookingDraft(null);

        assertThat(cleared.bookingDraft()).isNull();
    }

    @Test
    void withState_updatesTimestamp() {
        Session original = new Session("sess-1", "user-42", SessionState.IDLE, null,
                LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2020, 1, 1, 0, 0));

        Session updated = original.withState(SessionState.CONFIRMING);

        assertThat(updated.updatedAt()).isAfter(original.updatedAt());
    }
}
