package hr.lknezevic.barberbot.handler;

public interface HandlerPipeline<T> {
    void process(T value);
}
