package hr.lknezevic.barberbot.model;

import hr.lknezevic.barberbot.enums.SessionState;

import java.time.LocalDateTime;

public record Session(
        SessionState sessionState,
        String id,
        String userId,
        BookingDraft bookingDraft,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public Session withSessionState(SessionState newSessionState) {
        return new Session(
                newSessionState,
                this.id,
                this.userId,
                this.bookingDraft,
                this.createdAt,
                LocalDateTime.now()
        );
    }

    public Session withBookingDraft(BookingDraft newBookingDraft) {
        return new Session(
                this.sessionState,
                this.id,
                this.userId,
                newBookingDraft,
                this.createdAt,
                LocalDateTime.now()
        );
    }
}
