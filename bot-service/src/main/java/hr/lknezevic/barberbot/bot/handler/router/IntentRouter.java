package hr.lknezevic.barberbot.bot.handler.router;

import hr.lknezevic.barberbot.bot.handler.intent.IntentHandler;
import hr.lknezevic.barberbot.model.enums.LlmIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IntentRouter {

    private final Map<LlmIntent, IntentHandler> handlerMap;

    public IntentRouter(List<IntentHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(IntentHandler::supportedIntent, Function.identity()));
        log.info("Registered {} intent handlers: {}", handlerMap.size(), handlerMap.keySet());
    }

    public Optional<IntentHandler> route(LlmIntent intent) {
        return Optional.ofNullable(handlerMap.get(intent));
    }
}
