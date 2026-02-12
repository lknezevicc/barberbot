package hr.lknezevic.barberbot.model.dto;

import hr.lknezevic.barberbot.model.enums.SessionState;

import java.time.LocalDateTime;

public record Session(
        String id,
        String userId,
        SessionState state,
        BookingDraft bookingDraft,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public Session withBookingDraft(BookingDraft newDraft) {
        return new Session(id, userId, state, newDraft, createdAt, LocalDateTime.now());
    }

    public Session withState(SessionState newState) {
        return new Session(id, userId, newState, bookingDraft, createdAt, LocalDateTime.now());
    }
}
