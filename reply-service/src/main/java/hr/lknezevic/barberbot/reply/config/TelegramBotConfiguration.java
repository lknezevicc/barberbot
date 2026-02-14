package hr.lknezevic.barberbot.reply.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Configuration
class TelegramBotConfiguration {

    @Bean
    DefaultBotOptions telegramBotOptions() {
        return new DefaultBotOptions();
    }
}
