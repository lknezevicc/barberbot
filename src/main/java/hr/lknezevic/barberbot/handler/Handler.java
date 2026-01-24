package hr.lknezevic.barberbot.handler;

public interface Handler<T, R> {
    boolean supports(T value);
    R handle(T value);
}
