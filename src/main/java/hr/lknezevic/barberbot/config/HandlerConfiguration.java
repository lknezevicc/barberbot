package hr.lknezevic.barberbot.config;

import hr.lknezevic.barberbot.handler.update.UpdateHandler;
import hr.lknezevic.barberbot.handler.update.UpdateHandlerPipeline;
import hr.lknezevic.barberbot.service.BotMessageService;
import hr.lknezevic.barberbot.service.LlmService;
import hr.lknezevic.barberbot.service.SessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class HandlerConfiguration {

    @Bean
    UpdateHandlerPipeline updateHandlerPipeline(
            SessionService sessionService,
            LlmService llmService,
            BotMessageService botMessageService,
            List<UpdateHandler> handlers) {
        return new UpdateHandlerPipeline(sessionService, llmService, botMessageService, handlers);
    }
}
