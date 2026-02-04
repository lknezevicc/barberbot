package hr.lknezevic.barberbot.storage;

import java.util.Optional;

public interface StringSetStore {
    void add(String value);
    Optional<String> pop();
    void remove(String value);
}
