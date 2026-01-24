package hr.lknezevic.barberbot.service;

import hr.lknezevic.barberbot.enums.SessionState;
import hr.lknezevic.barberbot.model.Session;
import hr.lknezevic.barberbot.storage.KeyValueStore;
import hr.lknezevic.barberbot.storage.StringKeyValueStore;
import hr.lknezevic.barberbot.utils.ConstantsUtility;
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

    private static final Duration SESSION_TTL = Duration.ofMinutes(ConstantsUtility.SESSION_TTL_MINUTES);
    private final KeyValueStore<Session> sessionRedisKeyValueStore;
    private final StringKeyValueStore userSessionRedisStringKeyValueStore;

    public Session startSession(String userId) {
        Session session = new Session(
                SessionState.NONE,
                UUID.randomUUID().toString(),
                userId,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        sessionRedisKeyValueStore.save(session.id(), session, SESSION_TTL);
        userSessionRedisStringKeyValueStore.save(
                userId + ":session",
                session.id(),
                SESSION_TTL);

        return session;
    }

    public void refreshSession(Session session) {
        Session refreshedSession = new Session(
                session.sessionState(),
                session.id(),
                session.userId(),
                session.createdAt(),
                LocalDateTime.now()
        );

        sessionRedisKeyValueStore.save(refreshedSession.id(), refreshedSession, SESSION_TTL);
        userSessionRedisStringKeyValueStore.save(
                refreshedSession.userId() + ":session",
                session.id(),
                SESSION_TTL);
    }

    public void refreshSessionByUserId(String userId) {
        if (userId == null) return;
        getSessionByUserId(userId).ifPresent(this::refreshSession);
    }

    public Session setSessionState(Session session, SessionState newState) {
        Session updated = session.withSessionState(newState);
        sessionRedisKeyValueStore.save(updated.id(), updated, SESSION_TTL);
        userSessionRedisStringKeyValueStore.save(
                updated.userId() + ":session",
                updated.id(),
                SESSION_TTL
        );
        return updated;
    }

    public Optional<Session> getSession(String sessionId) {
        return sessionRedisKeyValueStore.get(sessionId);
    }

    public Optional<Session> getSessionByUserId(String userId) {
        return userSessionRedisStringKeyValueStore
                .get(userId + ":session")
                .flatMap(this::getSession);
    }
}
