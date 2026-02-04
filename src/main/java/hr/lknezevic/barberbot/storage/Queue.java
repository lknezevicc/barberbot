package hr.lknezevic.barberbot.storage;

import java.time.Duration;
import java.util.Optional;

public interface Queue<T> {
    void enqueue(T value);
    Optional<T> dequeue(String queueKey, Duration timeout);
    void moveToDlq(T value);
}
