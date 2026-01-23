package hr.lknezevic.barberbot.config;

import hr.lknezevic.barberbot.handler.update.SessionHandler;
import hr.lknezevic.barberbot.handler.update.UpdateHandler;
import hr.lknezevic.barberbot.handler.update.UpdateHandlerPipeline;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class HandlerConfiguration {

    @Bean
    UpdateHandlerPipeline updateHandlerPipeline(List<UpdateHandler> handlers) {
        return new UpdateHandlerPipeline(handlers);
    }
}
