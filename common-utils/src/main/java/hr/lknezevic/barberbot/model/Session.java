package hr.lknezevic.barberbot.model;

import java.time.LocalDateTime;

public record Session(
        String id,
        String userId,
        BookingDraft bookingDraft,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public Session withBookingDraft(BookingDraft newBookingDraft) {
        return new Session(
                this.id,
                this.userId,
                newBookingDraft,
                this.createdAt,
                LocalDateTime.now()
        );
    }
}
