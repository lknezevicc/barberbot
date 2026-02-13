package hr.lknezevic.barberbot.bot.service;

import hr.lknezevic.barberbot.infrastructure.storage.ObjectValueStore;
import hr.lknezevic.barberbot.infrastructure.storage.StringValueStore;
import hr.lknezevic.barberbot.model.dto.BookingDraft;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.SessionState;
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

    private final ObjectValueStore<Session> sessionObjectValueStore;
    private final StringValueStore userSessionStringValueStore;

    public Session getOrCreateSession(String userId) {
        return getSessionByUserId(userId).orElseGet(() -> startSession(userId));
    }

    public Session startSession(String userId) {
        Session session = new Session(
                UUID.randomUUID().toString(),
                userId,
                SessionState.IDLE,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        sessionObjectValueStore.save(session.id(), session, SESSION_TTL);
        userSessionStringValueStore.save(userId + ":session", session.id(), SESSION_TTL);

        return session;
    }

    public void saveSession(Session session) {
        sessionObjectValueStore.save(session.id(), session, SESSION_TTL);
    }

    public void refreshSession(String sessionId) {
        sessionObjectValueStore.refreshTTL(sessionId, SESSION_TTL);
    }

    public Optional<Session> getSessionByUserId(String userId) {
        return userSessionStringValueStore
                .get(userId + ":session")
                .flatMap(sessionId -> sessionObjectValueStore.get(sessionId));
    }
}
