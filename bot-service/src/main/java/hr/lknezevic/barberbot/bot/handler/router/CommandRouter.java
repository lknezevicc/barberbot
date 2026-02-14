package hr.lknezevic.barberbot.bot.handler.router;

import hr.lknezevic.barberbot.bot.handler.command.CommandHandler;
import hr.lknezevic.barberbot.model.enums.BotCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CommandRouter {

    private final Map<BotCommand, CommandHandler> handlerMap;

    public CommandRouter(List<CommandHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(CommandHandler::supportedCommand, Function.identity()));
        log.info("Registered {} command handlers: {}", handlerMap.size(), handlerMap.keySet());
    }

    public Optional<CommandHandler> route(BotCommand command) {
        return Optional.ofNullable(handlerMap.get(command));
    }
}
