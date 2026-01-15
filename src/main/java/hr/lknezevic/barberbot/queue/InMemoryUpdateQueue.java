package hr.lknezevic.barberbot.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public final class InMemoryUpdateQueue implements UpdateQueue {
    @Override
    public Update dequeue() {
        return null;
    }

    @Override
    public void enqueue(Update update) {

    }
}
