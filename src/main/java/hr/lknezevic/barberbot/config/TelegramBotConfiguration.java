package hr.lknezevic.barberbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Configuration
public class TelegramBotConfiguration {

    @Bean
    public DefaultBotOptions telegramBotOptions(Environment env) {
        DefaultBotOptions options = new DefaultBotOptions();

        if (env.acceptsProfiles(Profiles.of("dev"))) {
            options.setMaxThreads(4);
        }

        return options;
    }
}
