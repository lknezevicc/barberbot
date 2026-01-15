package hr.lknezevic.barberbot.queue;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateQueue {
    Update dequeue();
    void enqueue(Update update);
}
