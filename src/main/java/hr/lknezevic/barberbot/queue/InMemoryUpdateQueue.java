package hr.lknezevic.barberbot.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public final class InMemoryUpdateQueue implements UpdateQueue {

    private final BlockingQueue<Update> queue = new LinkedBlockingQueue<>();

    @Override
    public Update dequeue() throws InterruptedException {
        return queue.take();
    }

    @Override
    public void enqueue(Update update) {
        queue.offer(update);
    }
}
