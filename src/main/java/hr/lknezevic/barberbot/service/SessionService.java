package hr.lknezevic.barberbot.service;

import hr.lknezevic.barberbot.model.BookingDraft;
import hr.lknezevic.barberbot.model.Session;
import hr.lknezevic.barberbot.storage.ObjectValueStore;
import hr.lknezevic.barberbot.storage.StringValueStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private final ObjectValueStore<Session> sessionRedisObjectValueStore;
    private final StringValueStore userSessionRedisStringValueStore;

    public void updateSessionBookingDraft(Session session, BookingDraft bookingDraft) {
        Session updatedSession = session.withBookingDraft(bookingDraft);
        sessionRedisObjectValueStore.save(updatedSession.id(), updatedSession, SESSION_TTL);
    }

    public Session startSession(String userId) {
        Session session = new Session(
                UUID.randomUUID().toString(),
                userId,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        sessionRedisObjectValueStore.save(session.id(), session, SESSION_TTL);
        userSessionRedisStringValueStore.save(
                userId + ":session",
                session.id(),
                SESSION_TTL);

        return session;
    }

    public void refreshSession(String sessionId) {
        sessionRedisObjectValueStore.refreshTTL(sessionId, SESSION_TTL);
    }

    public void refreshSessionByUserId(String userId) {
        if (userId == null) return;
        getSessionByUserId(userId).ifPresent(session -> refreshSession(session.id()));
    }

    public Optional<Session> getSession(String sessionId) {
        return sessionRedisObjectValueStore.get(sessionId);
    }

    public Optional<Session> getSessionByUserId(String userId) {
        return userSessionRedisStringValueStore
                .get(userId + ":session")
                .flatMap(this::getSession);
    }
}
