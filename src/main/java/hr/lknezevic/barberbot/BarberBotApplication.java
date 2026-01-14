package hr.lknezevic.barberbot;

import hr.lknezevic.barberbot.config.ApplicationConfig;
import hr.lknezevic.barberbot.config.TelegramConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({TelegramConfig.class, ApplicationConfig.class})
@SpringBootApplication
public class BarberBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarberBotApplication.class, args);
    }

}
