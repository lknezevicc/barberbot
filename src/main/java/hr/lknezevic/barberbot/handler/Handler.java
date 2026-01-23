package hr.lknezevic.barberbot.handler;

public interface Handler<T> {
    boolean supports(T value);
    void handle(T value);
}
