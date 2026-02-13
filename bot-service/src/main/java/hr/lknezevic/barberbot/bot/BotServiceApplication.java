package hr.lknezevic.barberbot.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class BotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotServiceApplication.class, args);
    }
}
