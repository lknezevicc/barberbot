package hr.lknezevic.barberbot.model;

import hr.lknezevic.barberbot.enums.SessionState;

import java.time.LocalDateTime;

public record Session(
        SessionState sessionState,
        String id,
        String userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public Session withSessionState(SessionState newSessionState) {
        return new Session(
                newSessionState,
                this.id,
                this.userId,
                this.createdAt,
                LocalDateTime.now()
        );
    }
}
