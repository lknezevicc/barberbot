package hr.lknezevic.barberbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Configuration
public class TelegramBotConfiguration {

    @Bean
    @Profile("dev")
    DefaultBotOptions telegramBotOptionsDev() {
        DefaultBotOptions options = new DefaultBotOptions();
        options.setMaxThreads(4);
        return options;
    }

    @Bean
    @Profile("!dev")
    DefaultBotOptions telegramBotOptionsProd() {
        return new DefaultBotOptions();
    }
}
