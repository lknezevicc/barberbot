package hr.lknezevic.barberbot.telegram;

import hr.lknezevic.barberbot.queue.UpdateQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public final class TelegramWebhookController {
    private final UpdateQueue updateQueue;

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void onUpdate(@RequestBody Update update) {
        updateQueue.enqueue(update);
    }
}
