package hr.lknezevic.barberbot.queue;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateQueue {
    Update dequeue() throws InterruptedException;
    void enqueue(Update update);
}
